import google.generativeai as genai
import os
import json
import argparse
from pathlib import Path

# --- 프롬프트 (Prompt) ---
"""
전사, 요약, 해시태깅을 한번의 요청으로 모두 처리
모델 출력은 JSON 파일 형식이 되도록 작성함.
"""
PROMPT_TEMPLATE = """
You are a highly skilled AI assistant specialized in analyzing audio recordings. Your task is to process the attached audio file and provide a structured analysis in a single JSON object.

Perform the following three tasks:
1.  **transcription**: Accurately transcribe the entire audio into Korean text.
2.  **summary**: Based on the transcription, write a concise summary in Korean that covers the main points, discussions, and any decisions made. The summary should be presented in bullet points.
3.  **hashtags**: Generate an array of 5 to 7 relevant hashtags in Korean that best represent the key topics of the audio content.

Your final output MUST be a valid JSON object with the following structure and keys. Do not include any text outside of the JSON object.

{
  "transcription": "...",
  "summary": [
    "- ...",
    "- ..."
  ],
  "hashtags": ["#...", "#..."]
}
"""

flash = 'gemini-2.5-flash'
pro = 'gemini-2.5-pro'
MODEL_NAME = flash
audio_file_path = "sample_short.wav"

def init_model():
    # 사용할 모델을 초기화해 리턴
    try:
        return genai.GenerativeModel(model_name=MODEL_NAME)
    except Exception as e:
        print(f"모델 초기화 중 오류 발생 {e}")
        return None
    
def analyze_audio(model, audio_file_path):
    # 오디오 파일을 로드 및 업로드. 모델에게 프롬프트와 함께 전송해 분석 결과를 리턴함
    if not audio_file_path.is_file():
        print(f"오류: 파일을 찾을 수 없음 - {audio_file_path}")
        return None
    
    print(f"{audio_file_path} 업로드 중")
    try:
        audio_file = genai.upload_file(path=audio_file_path)
        print(f"{audio_file_path} 업로드 완료")
    except Exception as e:
        print(f"파일 업로드 중 오류 발생: {e}")
        return None
    
    print(f"분석 중 ({MODEL_NAME})")
    try:
        response = model.generate_content([PROMPT_TEMPLATE, audio_file])
        # 모델이 코드 블럭을 반환할 경우를 대비해, """"""을 지움
        clean_response_text = response.text.strip().replace("```json", "").replace("```", "")
        return json.loads(clean_response_text)
    except json.JSONDecodeError:
        print("오류: API 응답을 JSON 형식으로 파싱 불가")
        print("--- API Raw Response ---")
        print(response.text)
        return None
    except Exception as e:
        print(f"API 요청 중 오류 발생: {e}")
        return None
    finally:
        # 업로드한 파일은 이제 필요 없으므로 삭제
        if 'audio_file' in locals() and audio_file:
            genai.delete_file(audio_file.name)

def main():
    """프로그램의 메인 실행 함수."""
    parser = argparse.ArgumentParser(description="Gemini API를 사용하여 오디오 파일을 분석합니다.")
    parser.add_argument("file_path", type=str, help="분석할 오디오 파일의 경로")
    args = parser.parse_args()

    audio_path = Path(args.file_path)

    model = init_model()
    if not model: # 모델 초기화 실패시 종료
        return

    analysis_result = analyze_audio(model, audio_path)

    if analysis_result:
        print("\n--- 오디오 분석 결과 ---")
        print("\n[전체 텍스트 변환]")
        print(analysis_result.get("transcription", "내용 없음"))

        print("\n[핵심 내용 요약]")
        summary_points = analysis_result.get("summary", [])
        for point in summary_points:
            print(point)

        print("\n[해시태그 추천]")
        hashtags = analysis_result.get("hashtags", [])
        print(" ".join(hashtags))
        print("\n----------------------")

if __name__ == "__main__":
    main()