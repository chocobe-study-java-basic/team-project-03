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
	@python3 -c 'from pathlib import Path; import secrets; folder = Path("monitoring/secrets"); folder.mkdir(parents=True, exist_ok=True, mode=0o700); folder.chmod(0o700); target = folder / "prometheus-password"; target.exists() or target.write_text(secrets.token_hex(32) + "\n"); target.chmod(0o644)'
	@echo "monitoring/secrets/prometheus-password 파일을 준비했습니다. IntelliJ의 PROMETHEUS_PASSWORD에 파일 내용을 설정하세요."

monitoring-up:
	@test -s monitoring/secrets/prometheus-password || (echo "먼저 make monitoring-init을 실행하세요."; exit 1)
	docker compose --profile monitoring up -d prometheus

monitoring-down:
	docker compose --profile monitoring stop prometheus

monitoring-logs:
	docker compose --profile monitoring logs -f prometheus
