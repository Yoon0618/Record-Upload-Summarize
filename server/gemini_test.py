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
audio_file_path = "sample_long.wav"

# single turn
def single_turn(prompt: str, attachment=None, model_name: str = MODEL_NAME):
    model = genai.GenerativeModel(model_name) 
    response = model.generate_content([prompt, attachment])
    return response

# STT, Speech to Text
def STT(prompt: str = 'Please transcribe the following audio file', attachment = None, model_name: str = MODEL_NAME):
    # Gemini API에 오디오 파일 업로드
    print(f"'{audio_file_path}' 파일을 업로드하는 중입니다...")
    audio_file = genai.upload_file(path=audio_file_path)
    
    print(f"파일 업로드 완료: {audio_file.uri}")

    response = single_turn(prompt=prompt, attachment=audio_file, model_name=model_name)

    # 변환된 텍스트 출력
    if response and response.text:
        genai.delete_file(audio_file.name) # 업로드한 파일 삭제
        return response
    else:
        return 0    

# summarize STT result
def summarize(prompt: str, attachment=None, model_name: str = MODEL_NAME):
    
    return summarized_text


# auto hashtaging
def hashtag(prompt: str, attachment=None, model_name: str = MODEL_NAME):
    hashtags = []

    return hashtags
    
STT()