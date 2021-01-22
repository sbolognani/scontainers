import flask
import json
from typing import Any, Dict, Optional


def make_response(success: bool,
                  message: Optional[str] = None,
                  data: Optional[Any] = None,
                  status: int = 200) -> flask.Response:
    body: Dict[str, Any] = {"success": success}
    if message is not None:
        body["message"] = message
    if data is not None:
        body["data"] = data
    return flask.Response(json.dumps(body),
                          mimetype="application/json",
                          status=status)
