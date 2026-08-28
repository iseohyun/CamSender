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
- [x] 자체 서명 인증서 허용을 위한 `SslConfigHelper` 구현
- [x] `TransferManager`를 통한 전송 큐 및 상태 관리 시스템 구축
- [x] 전송 현황 패널(BottomSheet) UI 및 제어 기능 구현
- [x] 전송 성공 후 클린업(파일 삭제) 로직 적용
- [x] GitHub Push

## Milestone 5: UX 최적화 및 최종 검증
- [x] 전송 실패 건 자동 복구(Recovery) 로직 구현
- [x] 메인 FAB 전송 상태 배지(Badge) 카운터 추가
- [x] 전송 목록 이미지 썸네일(Thumbnail) 표시
- [x] 사용자 친화적 에러 메시지(Error Mapping) 적용
- [x] 전체 기능 최종 검증 및 GitHub Push
