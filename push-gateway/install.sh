#!/usr/bin/env bash
# GamEmpires push gateway installer for the "talk" VM (Ubuntu 24.04).
# Usage (in the folder with gateway.py, gamempires-push.service and the Firebase key):
#   sudo bash install.sh service-account.json
set -euo pipefail
KEY="${1:-service-account.json}"
cd "$(dirname "$0")"
[ "$(id -u)" = 0 ] || { echo "Run with sudo: sudo bash install.sh $KEY"; exit 1; }
[ -f gateway.py ] && [ -f gamempires-push.service ] || { echo "gateway.py / gamempires-push.service missing here"; exit 1; }

echo "== 1/5 Packages"
apt-get update -qq && apt-get install -y -qq python3 python3-cryptography >/dev/null

echo "== 2/5 Firebase key"
if [ -f "$KEY" ]; then
  python3 -c "import json,sys;d=json.load(open(sys.argv[1]));assert d['type']=='service_account' and d['project_id']=='gamempires-messenger', 'not the gamempires-messenger service account';print('   key OK for project', d['project_id'])" "$KEY"
  install -d -m 700 /etc/gamempires-push
  install -m 600 -o root -g root "$KEY" /etc/gamempires-push/service-account.json
  shred -u "$KEY" 2>/dev/null || rm -f "$KEY"
  echo "   stored in /etc/gamempires-push/service-account.json (root only); upload copy removed"
elif [ -f /etc/gamempires-push/service-account.json ]; then
  echo "   using existing /etc/gamempires-push/service-account.json"
else
  echo "   Firebase key not found ($KEY). Download it first (SETUP.md step 1)."; exit 1
fi

echo "== 3/5 Caddy location"
BIND=127.0.0.1
if systemctl is-active --quiet caddy 2>/dev/null; then
  MODE=host; echo "   Caddy runs on this VM"
elif command -v docker >/dev/null && docker ps --format '{{.Names}} {{.Image}}' | grep -qi caddy; then
  CADDY=$(docker ps --format '{{.Names}} {{.Image}}' | grep -i caddy | head -1 | cut -d' ' -f1)
  NETMODE=$(docker inspect -f '{{.HostConfig.NetworkMode}}' "$CADDY")
  if [ "$NETMODE" = host ]; then MODE=host; echo "   Caddy container '$CADDY' uses host network"
  else MODE=docker; BIND=$(ip -4 -o addr show docker0 | awk '{print $4}' | cut -d/ -f1); BIND=${BIND:-172.17.0.1}
       echo "   Caddy container '$CADDY' (network $NETMODE) -> gateway listens on $BIND"; fi
else
  MODE=unknown; echo "   Caddy not found automatically; using 127.0.0.1 (see SETUP.md step 3)"
fi

echo "== 4/5 Service"
install -d /opt/gamempires-push
install -m 644 gateway.py /opt/gamempires-push/gateway.py
sed "s/^Environment=PUSH_GATEWAY_BIND=.*/Environment=PUSH_GATEWAY_BIND=$BIND/" gamempires-push.service > /etc/systemd/system/gamempires-push.service
systemctl daemon-reload
systemctl enable --now gamempires-push >/dev/null
systemctl restart gamempires-push
sleep 2
if curl -fsS "http://$BIND:8790/health"; then echo; echo "   gateway running"; else
  echo "   gateway did not start:"; journalctl -u gamempires-push -n 30 --no-pager; exit 1; fi
journalctl -u gamempires-push -n 5 --no-pager | grep -E "Firebase project|ERROR" || true

echo "== 5/5 Public route"
if curl -fsS -m 10 https://talk.gamempires.com/push-gateway/health >/dev/null 2>&1; then
  echo "   https://talk.gamempires.com/push-gateway/health OK"
else
  echo "   Not published yet. Add this inside the talk.gamempires.com { ... } block of the Caddyfile,"
  echo "   ABOVE the existing reverse_proxy line, then reload Caddy:"
  echo
  echo "    handle_path /push-gateway/* {"
  echo "        reverse_proxy $BIND:8790"
  echo "    }"
  echo
  if [ "$MODE" = docker ]; then echo "   Reload: sudo docker exec $CADDY caddy reload --config /etc/caddy/Caddyfile"
  else echo "   Reload: sudo systemctl reload caddy"; fi
  echo "   Then check: curl -s https://talk.gamempires.com/push-gateway/health"
fi
if docker ps --format '{{.Names}}' 2>/dev/null | grep -qx nextcloud-aio-nextcloud; then
  if docker exec nextcloud-aio-nextcloud curl -fsS -m 10 https://talk.gamempires.com/push-gateway/health >/dev/null 2>&1; then
    echo "   Nextcloud container can reach the gateway: OK"
  else
    echo "   Nextcloud container cannot reach the gateway yet (fine until the Caddy route is added; re-check after)"
  fi
fi
echo "Done."
