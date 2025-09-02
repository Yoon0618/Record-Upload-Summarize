import os

# 운영체제에 상관없이 홈 디렉토리 경로를 가져옵니다.
home_directory = os.path.expanduser("~")

print(home_directory)