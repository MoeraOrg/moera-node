function hostChanged() {
    const host = $("#host").val();
    const valid = host.match(/^[a-z][a-z0-9-]*$/i);
    $("#host-submit").attr("disabled", !valid);
    $("#host").removeClass("is-valid is-invalid");
    if (host !== "") {
        $("#host").addClass(valid ? "is-valid" : "is-invalid");
    }
}

function registrarInit() {
    hostChanged();
    $("#host").keyup(hostChanged);
    $("#host").on("input", hostChanged);
}

$(registrarInit);
