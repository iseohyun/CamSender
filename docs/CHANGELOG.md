# Changelog

## [v0.3.0] - 2026-08-28
### Added
- Milestone 2 구현 완료: 서비스 탐색 (NSD) 시스템.
- `NsdHelper.kt`: NsdManager를 이용한 "CamSenderServer" 자동 탐색 로직.
- 포트 동적 처리: 탐색된 서비스의 포트 정보를 자동으로 인식 및 적용.
- `MainActivity.kt`: NSD 연동 및 서버 발견/유실에 따른 실시간 UI 업데이트.
- `.cursorrules` 설계 원칙 업데이트: 응집도/결합도 및 의도 확인 절차 명시.

## [v0.2.0] - 2026-08-28
### Added
- Milestone 1 구현 완료: 기초 설정 및 권한 시스템.
- Git 저장소 초기화 및 GitHub 원격 저장소(`main`) Push 완료.
- CameraX, OkHttp, Coroutines 라이브러리 의존성 설정.
- `ViewBinding` 활성화.
- `AndroidManifest.xml` 필수 권한 추가 (Camera, Internet, Nearby Devices 등).
- 기본 UI 레이아웃(`activity_main.xml`) 및 권한 요청 로직(`MainActivity.kt`) 구현.

## [v0.1.1] - 2026-08-28
### Added
- `.cursorrules` 추가: 작업 후 문서 자동 업데이트 규칙 수립.

## [v0.1.0] - 2026-08-28
### Added
- 프로젝트 초기 구조 설정.
- 문서화 폴더(`docs/`) 및 명세서 생성.
- XML ViewBinding 기반 Empty Views Activity 아키텍처 확정.
- 마일스톤 및 태스크 리스트 작성.
