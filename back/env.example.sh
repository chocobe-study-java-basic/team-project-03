#!/bin/bash
## 환경변수 초기화 실행 파일
## 배포 서버에서 실행하기: `source ~/budzet/env.sh`

export DB_URL='jdbc:mysql://DB_URI'
export DB_USERNAME='MySQL 계정'
export DB_PASSWORD='비밀번호'
export ACCESS_SECRET='JWT Secret'
export REFRESH_SECRET='JWT Secret'
export OCR_SPACE_API_KEY='API KEY'