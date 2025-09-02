import os
import subprocess
import uuid
from flask import Flask, request, jsonify
import google.generativeai as genai
import requests
import logging

# --- 기본 설정 ---
# 로깅 설정
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# Flask 앱 초기화
app = Flask(__name__)

# 업로드된 파일을 저장할 디렉토리
UPLOAD_FOLDER = 'uploads'
if not os.path.exists(UPLOAD_FOLDER):
    os.makedirs(UPLOAD_FOLDER)
app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER

# --- API 키 및 설정 (실제 값으로 변경해야 합니다) ---
# Gemini API 키 설정
# Google One AI Pro를 통해 얻은 API 키를 여기에 입력하세요.
# 환경 변수에서 가져오는 것을 권장합니다.
GEMINI_API_KEY = os.environ.get("GEMINI_API_KEY", "YOUR_GEMINI_API_KEY")
genai.configure(api_key=GEMINI_API_KEY)

# Notion API 설정
NOTION_API_KEY = os.environ.get("NOTION_API_KEY", "YOUR_NOTION_API_KEY")
NOTION_DATABASE_ID = os.environ.get("NOTION_DATABASE_ID", "YOUR_NOTION_DATABASE_ID")
NOTION_API_URL = "https://api.notion.com/v1/pages"

# --- 핵심 기능 함수 ---

def speech_to_text_whisper(audio_path):
    """
    OpenAI의 Whisper를 사용하여 오디오 파일을 텍스트로 변환합니다.
    서버에 whisper-cli가 설치되어 있어야 합니다. (pip install -U openai-whisper)
    """
    try:
        logging.info(f"Whisper STT 시작: {audio_path}")
        # whisper-cli를 직접 호출합니다. 모델은 'base'를 사용하여 속도를 확보합니다.
        # 더 높은 정확도를 원하면 'medium' 또는 'large' 모델을 사용할 수 있습니다.
        result = subprocess.run(
            ["whisper", audio_path, "--model", "base", "--language", "Korean", "--output_format", "txt"],
            capture_output=True, text=True, check=True
        )
        # 생성된 .txt 파일 경로를 계산합니다.
        transcript_path = audio_path.rsplit('.', 1)[0] + '.txt'
        with open(transcript_path, 'r', encoding='utf-8') as f:
            transcript = f.read()
        
        # 임시 텍스트 파일 삭제
        os.remove(transcript_path)
        logging.info("Whisper STT 성공")
        return transcript.strip()
    except subprocess.CalledProcessError as e:
        logging.error(f"Whisper STT 오류: {e.stderr}")
        return None
    except FileNotFoundError:
        logging.error("Whisper CLI를 찾을 수 없습니다. 'pip install -U openai-whisper'로 설치해주세요.")
        return None
    except Exception as e:
        logging.error(f"STT 중 예외 발생: {e}")
        return None

def summarize_and_organize_with_gemini(transcript):
    """
    Gemini API를 사용하여 텍스트를 요약하고, 핵심 포인트, 제목, 해시태그를 추출합니다.
    """
    if not transcript:
        return None

    try:
        logging.info("Gemini 분석 시작...")
        model = genai.GenerativeModel('gemini-1.5-flash')
        
        prompt = f"""
        다음 텍스트를 분석하여 아래 형식에 맞춰 JSON 객체로 정리해줘.
        - title: 전체 내용을 아우르는 핵심적인 제목 (5단어 이내)
        - summary: 전체 내용을 3-4문장으로 요약
        - key_points: 핵심 내용을 불렛포인트 형식의 배열(array)로 정리 (3~5개)
        - hashtags: 관련된 주제를 나타내는 해시태그 배열(array) (5개 이내, # 포함)

        --- 원본 텍스트 ---
        {transcript}
        --------------------

        JSON 형식으로만 응답해줘.
        """
        
        response = model.generate_content(prompt)
        # Gemini가 생성한 텍스트에서 JSON 부분만 추출
        # 때때로 모델이 ```json ... ``` 형식으로 감싸서 응답할 수 있으므로 이를 처리
        cleaned_response = response.text.strip().replace('```json', '').replace('```', '').strip()
        
        logging.info("Gemini 분석 성공")
        return cleaned_response
    except Exception as e:
        logging.error(f"Gemini API 호출 오류: {e}")
        return None

def add_to_notion(data, original_transcript):
    """
    처리된 데이터를 Notion 데이터베이스에 새 페이지로 추가합니다.
    """
    if NOTION_API_KEY == "YOUR_NOTION_API_KEY" or NOTION_DATABASE_ID == "YOUR_NOTION_DATABASE_ID":
        logging.warning("Notion API 키 또는 데이터베이스 ID가 설정되지 않았습니다.")
        return False
        
    headers = {
        "Authorization": f"Bearer {NOTION_API_KEY}",
        "Content-Type": "application/json",
        "Notion-Version": "2022-06-28"
    }
    
    # Notion 데이터베이스의 속성(컬럼) 이름에 맞춰야 합니다.
    # 예: '제목', '요약', '핵심 포인트', '태그', '원본'
    page_data = {
        "parent": {"database_id": NOTION_DATABASE_ID},
        "properties": {
            "제목": {"title": [{"text": {"content": data.get('title', '제목 없음')}}]},
            "요약": {"rich_text": [{"text": {"content": data.get('summary', '')}}]},
            "태그": {"multi_select": [{"name": tag.replace('#', '')} for tag in data.get('hashtags', [])]},
        },
        "children": [
            {
                "object": "block",
                "type": "heading_2",
                "heading_2": {"rich_text": [{"text": {"content": "핵심 포인트"}}]}
            },
            {
                "object": "block",
                "type": "bulleted_list_item",
                "bulleted_list_item": {
                    "rich_text": [{"text": {"content": point}} for point in data.get('key_points', [])]
                }
            },
            {
                "object": "block",
                "type": "heading_2",
                "heading_2": {"rich_text": [{"text": {"content": "전체 녹음 텍스트"}}]}
            },
            {
                "object": "block",
                "type": "paragraph",
                "paragraph": {"rich_text": [{"text": {"content": original_transcript}}]}
            }
        ]
    }
    
    try:
        logging.info("Notion에 데이터 전송 시작...")
        response = requests.post(NOTION_API_URL, headers=headers, json=page_data)
        response.raise_for_status()  # HTTP 오류가 발생하면 예외를 발생시킴
        logging.info("Notion에 데이터 전송 성공")
        return True
    except requests.exceptions.RequestException as e:
        logging.error(f"Notion API 호출 오류: {e}")
        logging.error(f"응답 내용: {response.text}")
        return False

# --- API 엔드포인트 ---

@app.route('/upload', methods=['POST'])
def upload_file():
    if 'file' not in request.files:
        return jsonify({"error": "파일이 없습니다."}), 400
    
    file = request.files['file']
    if file.filename == '':
        return jsonify({"error": "선택된 파일이 없습니다."}), 400
        
    if file:
        # 고유한 파일 이름 생성
        filename = str(uuid.uuid4()) + os.path.splitext(file.filename)[1]
        filepath = os.path.join(app.config['UPLOAD_FOLDER'], filename)
        file.save(filepath)
        logging.info(f"파일 저장 완료: {filepath}")

        # STT 처리
        transcript = speech_to_text_whisper(filepath)
        if not transcript:
            return jsonify({"error": "음성을 텍스트로 변환하는 데 실패했습니다."}), 500
            
        # Gemini로 요약 및 정리
        import json
        processed_data_str = summarize_and_organize_with_gemini(transcript)
        if not processed_data_str:
            return jsonify({"error": "텍스트를 분석하는 데 실패했습니다."}), 500
        
        try:
            processed_data = json.loads(processed_data_str)
        except json.JSONDecodeError:
            logging.error(f"Gemini 응답 JSON 파싱 실패: {processed_data_str}")
            return jsonify({"error": "AI 응답을 파싱하는 데 실패했습니다."}), 500

        # Notion에 추가
        success = add_to_notion(processed_data, transcript)
        
        # 처리 완료 후 업로드된 오디오 파일 삭제
        os.remove(filepath)
        logging.info(f"임시 파일 삭제: {filepath}")

        if success:
            return jsonify({"message": "성공적으로 처리되어 Notion에 추가되었습니다."}), 200
        else:
            return jsonify({"error": "Notion에 추가하는 데 실패했습니다."}), 500

@app.route('/')
def index():
    return "AI 요약 서버가 실행 중입니다."

# --- 서버 실행 ---
if __name__ == '__main__':
    # host='0.0.0.0'으로 설정하여 외부에서 접근 가능하게 합니다.
    app.run(host='0.0.0.0', port=5000, debug=True)
