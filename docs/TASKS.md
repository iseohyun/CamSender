# Development Tasks

## Milestone 1: 기초 설정 및 권한
- [x] Android 프로젝트 문서 체계 수립 (SPEC, ARCHITECTURE, CHANGELOG, TASKS)
- [x] AI Agent Rule 설정 (`.cursorrules`)
- [ ] AndroidManifest.xml 권한 추가 (Camera, Internet, Nearby Devices)
- [ ] ViewBinding 활성화 (`build.gradle.kts`)
- [ ] 기본 레이아웃 구성 (PreviewView, Capture 버튼, Server Status)

## Milestone 2: 서비스 탐색 (NSD)
- [ ] `NsdHelper` 클래스 구현 (Service Discovery)
- [ ] 탐색된 서버 리스트 UI 연동
- [ ] 수동 IP 입력 다이얼로그 구현

## Milestone 3: 카메라 (CameraX)
- [ ] CameraProvider 초기화 및 Preview 연결
- [ ] `takePicture` 로직 구현 (cacheDir 저장)

## Milestone 4: 네트워크 전송 (OkHttp)
- [ ] 자체 서명 인증서 허용을 위한 `UnsafeOkHttpClient` 또는 SSLContext 설정
- [ ] Multipart POST 전송 함수 구현
- [ ] 전송 후 파일 삭제 로직 (Cleanup)

## Milestone 5: 테스트 및 고도화
- [ ] 전송 상태 UI (Loading, Success/Fail) 처리
- [ ] 에러 핸들링 (네트워크 단절, 서버 타임아웃 등)
