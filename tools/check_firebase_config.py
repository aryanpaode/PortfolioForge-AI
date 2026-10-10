#!/usr/bin/env python3
"""Validate shape/package only; never print keys or claim backend authentication works."""
from pathlib import Path
import json
p=Path(__file__).resolve().parents[1]/'app/google-services.json'
if not p.is_file():raise SystemExit('No app/google-services.json. Local mode works; add your Firebase Android configuration to enable auth.')
try:data=json.loads(p.read_text())
except (OSError,ValueError) as e:raise SystemExit('The Firebase configuration cannot be read as JSON.') from e
expected='com.aryanpaode.portfolioforgeai'
clients=[c for c in data.get('client',[]) if c.get('client_info',{}).get('android_client_info',{}).get('package_name')==expected]
if not clients:raise SystemExit('No Android client matches '+expected+'. Download the configuration for that exact package.')
client=clients[0]
if not data.get('project_info',{}).get('project_id'):raise SystemExit('Missing project_id.')
if not client.get('client_info',{}).get('mobilesdk_app_id'):raise SystemExit('Missing mobilesdk_app_id.')
if not any(x.get('current_key') for x in client.get('api_key',[])):raise SystemExit('Missing Firebase client API key field.')
print('Configuration structure and Android package match. No keys printed. Enable Email/Password in Firebase, sync Gradle, and test sign-in on a device.')
