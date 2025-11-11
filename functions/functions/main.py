# Welcome to Cloud Functions for Firebase for Python!
# To get started, simply uncomment the below code or create your own.
# Deploy with `firebase deploy`

from firebase_functions import https_fn
from firebase_functions.options import set_global_options
from firebase_admin import initialize_app, firestore

# For cost control, you can set the maximum number of containers that can be
# running at the same time. This helps mitigate the impact of unexpected
# traffic spikes by instead downgrading performance. This limit is a per-function
# limit. You can override the limit for each function using the max_instances
# parameter in the decorator, e.g. @https_fn.on_request(max_instances=5).
set_global_options(max_instances=10)

initialize_app()


@https_fn.on_request()
def revert_user_upgrade(req: https_fn.Request) -> https_fn.Response:
    """
    Request JSON: { "userId": "<uid>" }
    Sets isPro=false and clears planType for the given user.
    """
    try:
        data = req.get_json(silent=True) or {}
        user_id = data.get("userId") or req.args.get("userId")
        if not user_id:
            return https_fn.Response("Missing userId", status=400)

        db = firestore.client()
        user_ref = db.collection("users").document(user_id)
        user_ref.update({
            "isPro": False,
            "planType": None,
            "proSince": None,
            "lastUpdated": firestore.SERVER_TIMESTAMP,
        })
        return https_fn.Response("Reverted upgrade", status=200)
    except Exception as e:
        return https_fn.Response(f"Error: {e}", status=500)
