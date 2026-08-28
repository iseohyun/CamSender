# Changelog

## [v1.1.0] - 2026-08-28
### Added
- OTP 페어링 시스템 도입: 서버와의 최초 연결 시 6자리 PIN 번호를 통한 상호 인증 기능 추가.
- 지능형 동적 핀닝(TOFU): 인증 성공 시 서버 인증서(`cert.pem`)를 자동 다운로드 및 영구 저장하여 이후 통신에 적용.
- 자동 갱신 로직: 서버 인증서 변경 감지 시 자동으로 재페어링 유도.
- `DynamicSslManager.kt`: 인증서 획득, 저장, 및 핀닝 클라이언트 생성 전담 모듈 추가.
- 보안 강화: 수동 파일 복사 없이도 로컬 네트워크에서 중간자 공격(MITM) 방지 및 SSL 상호 검증 실현.

## [v1.0.5] - 2026-08-28
### Fixed
- 카메라 바인딩 안정화: 하드웨어 제약으로 인한 `IllegalArgumentException` 해결을 위해 유연한 해상도 전략 및 폴백(Preview-only) 바인딩 도입.
- 서버 연결 진단 강화: 인트로 화면 및 로그캣에 상세 연결 시퀀스 로그 추가.
- 헬스체크 투명성 확보: `TransferManager` 내부에 HTTP 요청/응답 상세 로깅 추가.

## [v1.0.4] - 2026-08-28
### Added
- 서버 주소 자동 완성 기능: 수동 설정 다이얼로그 오픈 시 현재 연결 정보 또는 과거 연결 이력(SharedPreferences)을 자동으로 입력.
- 연결 성공 시 서버 주소 영구 저장 로직 추가.

## [v1.0.3] - 2026-08-28
### Added
- 맞춤형 앱 아이콘 적용: 제공된 SVG 디자인을 바탕으로 Adaptive Icon(Background/Foreground) 구성.
- `colors.xml`에 아이콘 전용 테마 색상 추가.

## [v1.0.2] - 2026-08-28
### Added
- 사이드 패널(Drawer) 내 서버 수동 설정 버튼 추가: 자동 탐색 실패 시 즉각적인 대응 가능.
- `showManualConfigDialog` 로직 재사용을 통한 일관된 설정 환경 제공.

## [v1.0.1] - 2026-08-28
### Fixed
- UI 개편 후 발생한 뷰 참조 오류 수정: `etServerIp`, `tvBadge`, `btnConnect` 등 누락된 참조 해결.
- `activity_main.xml`에 드로어 오픈용 메뉴 버튼 및 상태 배지 추가.
- `MainActivity.kt` 로직 최적화 및 뷰 바인딩 정합성 확보.

## [v1.0.0] - 2026-08-28
### Added
- UI 전면 개편: `DrawerLayout` 기반의 사이드 패널 관리 시스템 도입.
- 네비게이션 드로어: 화면 너비의 95%를 차지하는 관리 패널 (왼쪽 스와이프로 열기).
- Safe Area 적용: `fitsSystemWindows` 및 인셋 처리를 통해 시스템 바 영역 보호.
- 전송 내역 고도화: 최신순 정렬, 촬영 일시 표시, 중지(Hold) 및 리스트 삭제 기능 추가.
- Full Camera View: 메인 화면을 카메라 프리뷰와 플로팅 촬영 버튼으로만 구성.
- 상태 배지: 사이드 패널 버튼(FAB)에 실시간 미처리 항목 개수 표시.

## [v0.9.0] - 2026-08-28
### Added
- Intro 화면(Searching Overlay) 도입: 앱 실행 시 서버 탐색 상태 및 실시간 로그 표시.
- 수동 등록 시스템: 탐색 대기 없이 다이얼로그를 통해 서버 IP/Port 직접 입력 가능.
- 카메라 호환성 개선: `IllegalArgumentException` 해결을 위한 해상도 전략 최적화 및 폴백(Fallback) 바인딩 로직 추가.
- 실시간 로그 피드백: Intro 화면 내 `ScrollView`를 통해 탐색 및 초기화 과정을 시각화.

## [v0.8.0] - 2026-08-28
### Added
- Milestone 7 구현 완료: 이미지 최적화 및 리소스 관리.
- CameraX 해상도 전략 적용: `ResolutionSelector`를 사용하여 출력 해상도를 1920x1440으로 최적화.
- JPEG 압축률 조정: `ImageCapture` 품질을 80으로 설정하여 장당 300~600KB 수준으로 용량 절감.
- 성능 개선: 불필요하게 높은 해상도 캡처를 지양함으로써 처리 속도 및 전송 대역폭 효율 확보.

## [v0.7.0] - 2026-08-28
### Added
- Milestone 6 구현 완료: 서버 연동 고도화 및 보안 강화.
- `network_security_config.xml`: `cert.pem`을 통한 정적 인증서 핀닝(Trust Anchor) 구축.
- 헬스체크 연동: 전송 전 서버의 `/health` 상태 및 저장소 준비 여부 검증 로직 추가.
- 동적 API 경로: NSD TXT 레코드에서 `api` 경로를 읽어와 전송 시 자동 적용.
- 보안 강화: `UnsafeOkHttpClient`를 제거하고 안드로이드 표준 보안 프레임워크 사용.
- `NsdHelper.kt`: TXT 레코드 파싱 및 버전/API 정보 추출 기능 추가.

## [v0.6.0] - 2026-08-28
### Added
- Milestone 5 구현 완료: UX 최적화 및 자동 복구 시스템.
- 자동 복구 로직: 앱 실행/서버 연결 시 `cacheDir` 스캔 및 미전송 파일 큐 등록.
- FAB 전송 상태 배지: 메인 화면 버튼에 대기/진행/실패 수 실시간 표시.
- 이미지 썸네일: 전송 목록 패널에 촬영 이미지 미리보기 구현.
- 한국어 에러 매핑: 네트워크 예외를 사용자 친화적인 메시지로 변환.
- 전송 목록 한글화 및 UI 피드백 강화.

## [v0.5.0] - 2026-08-28
### Added
- Milestone 4 구현 완료: HTTPS 전송 및 큐(Queue) 관리 시스템.
- `TransferManager.kt`: 백그라운드 전송 큐 및 상태(대기, 전송 중, 성공, 실패, 보류) 관리 로직.
- `SslConfigHelper.kt`: 자체 서명 인증서를 신뢰하는 OkHttpClient 설정 (개발용).
- `TransferStatusBottomSheet.kt`: 전송 현황 시각화 및 제어(재전송, 보류, 삭제)를 위한 하단 패널 UI.
- 전송 성공 시 로컬 파일 자동 삭제 및 실패 시 보관 정책 적용.
- `MainActivity`: 촬영 후 자동 전송 큐 등록 연동.

## [v0.4.0] - 2026-08-28
### Added
- Milestone 3 구현 완료: CameraX 카메라 기능.
- `CameraHelper.kt`: CameraX 유즈케이스(Preview, ImageCapture) 캡슐화.
- 4:3 비율 고정 프리뷰: 화면 잘림 없는 UI 레이아웃 적용.
- 플래시 모드 제어: ON/OFF/AUTO 순환 토글 기능.
- 터치 투 포커스: 화면 터치 지점 초점 및 노출 조정.
- 회전 지원: 기기 방향에 따른 사진 촬영 방향 자동 보정.
- `cacheDir` 저장: 촬영된 이미지를 임시 캐시 디렉토리에 저장.

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
