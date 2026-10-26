.PHONY: \
	dev-up \
	dev-down \
	dev-down-reset \
	db-logs \
	monitoring-init \
	monitoring-up \
	monitoring-down \
	monitoring-logs

# Docker Compose 서비스 실행
dev-up:
	docker compose up -d

# Docker Compose 컨테이너와 네트워크 종료 및 삭제 (볼륨 유지)
dev-down:
	docker compose down

# Docker Compose 컨테이너, 네트워크, named volume 전체 삭제
dev-down-reset:
	docker compose down -v

# MySQL 컨테이너 로그 실시간 확인 (Ctrl+C로 종료)
db-logs:
	docker compose logs -f mysql

# 기존 비밀번호가 있으면 덮어쓰지 않는다. 디렉터리는 현재 사용자만 접근한다.
monitoring-init:
	@python3 -c 'from pathlib import Path; import secrets; folder = Path("monitoring/secrets"); folder.mkdir(parents=True, exist_ok=True, mode=0o700); folder.chmod(0o700); targets = [folder / name for name in ("prometheus-password", "grafana-admin-password")]; [(target.exists() or target.write_text(secrets.token_hex(32) + "\n"), target.chmod(0o644)) for target in targets]'
	@echo "monitoring/secrets/에 비밀번호 파일을 준비했습니다. 기존 파일은 유지합니다."
	@echo "prometheus-password: IntelliJ의 PROMETHEUS_PASSWORD / grafana-admin-password: Grafana admin 로그인"

monitoring-up:
	@test -s monitoring/secrets/prometheus-password || (echo "먼저 make monitoring-init을 실행하세요."; exit 1)
	@test -s monitoring/secrets/grafana-admin-password || (echo "먼저 make monitoring-init을 실행하세요."; exit 1)
	docker compose --profile monitoring up -d prometheus grafana

monitoring-down:
	docker compose --profile monitoring stop grafana prometheus

monitoring-logs:
	docker compose --profile monitoring logs -f prometheus grafana
