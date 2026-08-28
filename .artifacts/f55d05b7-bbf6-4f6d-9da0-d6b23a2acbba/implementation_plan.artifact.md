# Milestone 1: 기초 설정 및 권한 구현 계획

마일스톤 1은 프로젝트의 기반을 다지는 단계입니다. CameraX, OkHttp 등 필수 라이브러리를 설정하고, 필요한 권한을 획득하며, 기본 UI 구조를 잡는 것을 목표로 합니다.

## User Review Required

> [!IMPORTANT]
> - **권한 요청:** 앱 실행 시 카메라 및 로컬 네트워크 탐색을 위한 권한 팝업이 노출됩니다.
> - **ViewBinding:** XML 기반 UI 제어를 위해 ViewBinding을 사용합니다.

## Proposed Changes

### [Build Configuration]

#### [MODIFY] [libs.versions.toml](file:///C:/git/CamSender/gradle/libs.versions.toml)
- CameraX, OkHttp, Coroutines 관련 버전 및 라이브러리 정의 추가.

#### [MODIFY] [build.gradle.kts (app)](file:///C:/git/CamSender/app/build.gradle.kts)
- `viewBinding` 활성화.
- `libs.versions.toml`에 정의한 라이브러리 의존성 추가.

### [Manifest & Permissions]

#### [MODIFY] [AndroidManifest.xml](file:///C:/git/CamSender/app/src/main/AndroidManifest.xml)
- `CAMERA`, `INTERNET`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`, `NEARBY_WIFI_DEVICES` 권한 추가.
- 하드웨어 가속 및 카메라 기능 선언.

### [UI & Activity]

#### [MODIFY] [activity_main.xml](file:///C:/git/CamSender/app/src/main/res/layout/activity_main.xml)
- `PreviewView` (카메라 프리뷰), `ImageButton` (촬영), `TextView` (서버 상태), `EditText` (수동 IP 입력) 포함 레이아웃 구성.

#### [MODIFY] [MainActivity.kt](file:///C:/git/CamSender/app/src/main/java/com/example/camsender/MainActivity.kt)
- ViewBinding 초기화.
- 런타임 권한 요청 로직 구현.
- 권한 획득 후 카메라 프리뷰 준비 (간단한 Placeholder 로직).

## Verification Plan

### Automated Tests
- 없음 (UI 및 설정 단계)

### Manual Verification
1. 앱 실행 시 카메라 권한 요청 확인.
2. 권한 허용 후 메인 UI(프리뷰 영역, 버튼 등)가 정상적으로 표시되는지 확인.
3. 빌드 성공 여부 확인.
