import crypto from 'crypto';

function secure_link(r) {
    const exp = r.args.exp;
    const fn = r.args.fn != null ? r.args.fn : "";
    const signature = r.args.sig;

    if (!exp || !signature) {
        return "bad";
    }

    const expires = Number(exp);

    if (!Number.isFinite(expires)) {
        return "bad";
    }

    const now = Math.floor(Date.now() / 1000);

    if (expires < now) {
        return "expired";
    }

    const expected = crypto
        .createHmac("sha256", r.variables.secure_link_secret)
        .update(r.variables.id + exp + fn)
        .digest("base64url");

    if (expected !== signature) {
        return "bad";
    }

    return "ok";
}

export default { secure_link };
