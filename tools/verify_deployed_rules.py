"""Run locally by the project owner after publishing rules. No credential is saved or logged."""
import getpass
import json
import sys
import urllib.request
import urllib.error
import urllib.parse
from pathlib import Path

PROJECT = "sportpro-df6e0"

def request_status(url, token=None):
    headers = {} if token is None else {"Authorization": "Bearer " + token}
    request = urllib.request.Request(url, headers=headers)
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            return response.status  # Do not read or export the profile body.
    except urllib.error.HTTPError as error:
        return error.code

def verify(project, uid, token, request_fn=request_status):
    if project != PROJECT or not uid or not token:
        raise ValueError("WRONG_PROJECT_OR_MISSING_IDENTITY")
    url = "https://firestore.googleapis.com/v1/projects/" + project + "/databases/(default)/documents/users/" + urllib.parse.quote(uid, safe="")
    allowed = request_fn(url, token)
    denied = request_fn(url, None)
    return {"project": project, "own_profile_authenticated_status": allowed,
            "same_profile_anonymous_status": denied,
            "pass": allowed == 200 and denied in (401, 403),
            "profile_body_exported": False, "credentials_exported": False}

def self_test():
    calls = []
    def fake(url, token):
        calls.append((url, token is not None))
        return 200 if token is not None else 403
    assert verify(PROJECT, "synthetic-user", "synthetic-token", fake)["pass"]
    assert [c[1] for c in calls] == [True, False]
    assert not verify(PROJECT, "synthetic-user", "synthetic-token", lambda *_:200)["pass"]
    assert not verify(PROJECT, "synthetic-user", "synthetic-token", lambda *_:403)["pass"]
    try:
        verify("other-project", "u", "t", fake)
        raise AssertionError("wrong project accepted")
    except ValueError:
        pass
    return {"self_test":"PASS", "real_network":False, "cases":4}

def main():
    if sys.argv[1:] == ["--self-test"]:
        print(json.dumps(self_test()));return 0
    if sys.argv[1:]:
        raise ValueError("NO_CREDENTIALS_IN_ARGUMENTS")
    config = json.loads((Path(__file__).resolve().parents[1] / "app/google-services.json").read_text())
    project = config["project_info"]["project_id"]
    if project != PROJECT:
        raise ValueError("WRONG_PROJECT")
    api_key = config["client"][0]["api_key"][0]["current_key"]
    email = input("Email de tu cuenta autorizada de prueba: ").strip()
    password = getpass.getpass("Contraseña (no se guarda ni se imprime): ")
    data = json.dumps({"email":email,"password":password,"returnSecureToken":True}).encode()
    request = urllib.request.Request("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + urllib.parse.quote(api_key, safe=""),
        data=data, headers={"Content-Type":"application/json"})
    with urllib.request.urlopen(request, timeout=20) as response:
        auth = json.load(response)
    result = verify(project, auth["localId"], auth["idToken"])
    print(json.dumps(result, indent=2))
    return 0 if result["pass"] else 90

if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as error:
        print(json.dumps({"status":"HOLD", "error_type":type(error).__name__, "http_status":getattr(error,"code",None), "raw_error_not_printed":True}))
        sys.exit(90)
