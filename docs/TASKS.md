# Development Tasks

## Milestone 1: 기초 설정 및 권한
- [x] Android 프로젝트 문서 체계 수립 (SPEC, ARCHITECTURE, CHANGELOG, TASKS)
- [x] AI Agent Rule 설정 (`.cursorrules`)
- [x] AndroidManifest.xml 권한 추가 (Camera, Internet, Nearby Devices)
- [x] ViewBinding 활성화 (`build.gradle.kts`)
- [x] 기본 레이아웃 구성 (PreviewView, Capture 버튼, Server Status)
- [x] MainActivity 권한 요청 로직 구현
- [x] Git 초기화 및 GitHub Push

## Milestone 2: 서비스 탐색 (NSD)
- [x] `NsdHelper` 클래스 구현 (Service Discovery, Filter, Resolve)
- [x] 탐색된 서버 리스트 UI 연동 및 동적 포트 적용
- [x] 수동 IP 입력 및 자동 탐색 우선순위 로직
- [x] GitHub Push

## Milestone 3: 카메라 (CameraX)
- [x] `CameraHelper` 클래스 구현 (Lifecycle, Preview, Capture, Flash, Focus)
- [x] 4:3 비율 고정 및 잘림 없는 UI 레이아웃 구성
- [x] 플래시 ON/OFF/AUTO 및 터치 투 포커스 구현
- [x] 가로/세로 촬영 방향 대응
- [x] GitHub Push

## Milestone 4: 네트워크 전송 (OkHttp)
- [ ] 자체 서명 인증서 허용을 위한 `UnsafeOkHttpClient` 또는 SSLContext 설정
- [ ] Multipart POST 전송 함수 구현
- [ ] 전송 후 파일 삭제 로직 (Cleanup)

## Milestone 5: 테스트 및 고도화
- [ ] 전송 상태 UI (Loading, Success/Fail) 처리
- [ ] 에러 핸들링 (네트워크 단절, 서버 타임아웃 등)
