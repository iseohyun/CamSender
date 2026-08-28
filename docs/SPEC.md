# CamSender Project Specification

## 1. 개요
Android 기기에서 촬영한 문서를 로컬 네트워크의 Python 서버로 HTTPS를 통해 안전하게 전송하고, 서버에서 이를 처리(OCR, DB 인덱싱)하는 시스템.

## 2. 안드로이드 앱 (Android)
- **UI Framework:** XML ViewBinding (Empty Views Activity)
- **Service Discovery:**
    - `NsdManager`를 사용하여 `_http._tcp` 타입의 서비스 자동 탐색.
    - **서비스 이름 필터링**: "CamSenderServer"가 포함된 서비스만 연결 대상으로 인식.
    - **동적 포트 처리**: 탐색된 서비스의 포트 정보를 실시간으로 반영.
    - 서버 IP/Port 수동 입력 필드 및 연결 테스트 기능 제공.
- **카메라 기능 (CameraX):**
    - 실시간 프리뷰 (PreviewView).
    - **고정 화면비**: 센서 비율에 따른 4:3 비율 고정 및 FIT_CENTER 레이아웃 (잘림 방지).
    - **플래시 제어**: ON / OFF / AUTO 토글 지원.
    - **터치 투 포커스**: 화면 터치 지점 초점 및 노출 조정.
    - **회전 지원**: 기기 방향에 따른 사진 촬영 방향 자동 보정.
    - `ImageCapture`를 통한 고화질 사진 촬영.
- **이미지 관리:**
    - 촬영된 이미지는 `context.cacheDir`에 임시 저장.
    - 파일명 규칙: `IMG_yyyyMMdd_HHmmss.jpg`
- **전송 기능 (OkHttp):**
    - HTTPS POST Multipart 전송.
    - **인증서 관리**: 서버에서 제공하는 특정 인증서만 신뢰하도록 동적 핀닝(Pinning) 지원 예정.
    - **전송 큐(Queue) 관리**:
        - 촬영 즉시 큐에 추가되어 백그라운드에서 순차적 전송.
        - 성공, 실패, 보류(Hold) 상태 관리 및 재전송 기능 제공.
- **이미지 및 데이터 정책:**
    - 촬영된 이미지는 `context.cacheDir`에 임시 저장.
    - 파일명 규칙: `IMG_yyyyMMdd_HHmmss.jpg`
    - **삭제 정책**: 서버 전송 성공 응답 수신 시 즉시 로컬 임시 파일 삭제. 전송 실패 시 보관 및 재전송 대기.

## 3. 서버 (Python FastAPI)
- **Framework:** FastAPI + Uvicorn.
- **Service Broadcast:** `Zeroconf` 라이브러리를 사용하여 로컬망에 서비스 알림.
- **Protocol:** HTTPS (Port: 8443).
- **Endpoint:** `POST /upload` (Multipart/form-data).
- **Pipeline:**
    - 이미지 수신 및 저장.
    - OCR 엔진(Tesseract 등)을 통한 텍스트 추출.
    - 추출된 데이터를 DB에 인덱싱.
