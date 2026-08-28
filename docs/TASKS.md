# Development Tasks - Integration & Optimization

## Milestone 6: 서버 연동 고도화 및 보안 강화
- [x] **Trust Anchor 기반 보안 통신 구현**
    - [x] 서버 인증서(`cert.pem`)를 `res/raw`에 배치
    - [x] `network_security_config.xml`을 통한 인증서 핀닝 적용
- [x] **서버 헬스체크(Health Check) 연동**
    - [x] 전송 전 `/health` 엔드포인트 호출 및 상태 검증 로직 추가
- [x] **동적 엔드포인트 연동**
    - [x] mDNS TXT 레코드에서 `api` 속성 읽기 및 적용
- [x] **보안 클린업**
    - [x] `UnsafeOkHttpClient` 제거 및 표준 `OkHttpClient` 전환
- [x] GitHub Push

## Milestone 7: 이미지 최적화 및 리소스 관리
- [ ] **CameraX 해상도 전략 적용**
    - [ ] `ResolutionSelector`를 통한 Target Resolution (1920x1440) 설정
    - [ ] `ImageCapture` JPEG Quality 80 설정
- [ ] **최적화 결과 검증**
    - [ ] 촬영 후 파일 용량(약 500KB 내외) 확인 테스트
    - [ ] 해상도 정보(1920x1440) 확인 테스트
