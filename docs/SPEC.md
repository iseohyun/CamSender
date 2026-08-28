# CamSender Project Specification

## 1. 개요
Android 기기에서 촬영한 문서를 로컬 네트워크의 Python 서버로 HTTPS를 통해 안전하게 전송하고, 서버에서 이를 처리(OCR, DB 인덱싱)하는 시스템.

## 2. 안드로이드 앱 (Android)
- **UI Framework:** XML ViewBinding (Empty Views Activity)
- **Service Discovery:**
    - `NsdManager`를 사용하여 `_http._tcp` 타입의 서비스 자동 탐색.
    - 서버 IP/Port 수동 입력 필드 및 연결 테스트 기능 제공.
- **카메라 기능 (CameraX):**
    - 실시간 프리뷰 (PreviewView).
    - `ImageCapture`를 통한 고화질 사진 촬영.
- **이미지 관리:**
    - 촬영된 이미지는 `context.cacheDir`에 임시 저장.
    - 파일명 규칙: `IMG_yyyyMMdd_HHmmss.jpg`
- **전송 기능 (OkHttp):**
    - HTTPS POST Multipart 전송.
    - 서버의 자체 서명 SSL 인증서(Self-signed)를 신뢰하도록 설정 필요.
- **후처리:**
    - 서버 응답 수신 즉시 (성공/실패 여부와 관계없이) 로컬 임시 파일 삭제.

## 3. 서버 (Python FastAPI)
- **Framework:** FastAPI + Uvicorn.
- **Service Broadcast:** `Zeroconf` 라이브러리를 사용하여 로컬망에 서비스 알림.
- **Protocol:** HTTPS (Port: 8443).
- **Endpoint:** `POST /upload` (Multipart/form-data).
- **Pipeline:**
    - 이미지 수신 및 저장.
    - OCR 엔진(Tesseract 등)을 통한 텍스트 추출.
    - 추출된 데이터를 DB에 인덱싱.
