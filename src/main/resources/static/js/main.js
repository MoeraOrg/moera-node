function initIndex() {
    $("#register-submit").click(function (event) {
        event.preventDefault();
        let data = JSON.stringify({
            "name": $("#register-name").val()
        });
        $.ajax({
            method: "POST",
            url: "/moera-node/registered-name",
            contentType: "application/json",
            data: data
        });
        $("#register-name").val("");
    });
}

function alertHide() {
    $(".alert").fadeOut();
}

function alertSuccess(text) {
    $(".alert").css("display", "none");
    $(".alert-success").text(text).css("display", "block");
    window.setTimeout(alertHide, 5000);
}

function alertFailure(text) {
    $(".alert").css("display", "none");
    $(".alert-danger").text(text).css("display", "block");
    window.setTimeout(alertHide, 5000);
}

function createCredentials(login, password) {
        let data = JSON.stringify({
            "login": login,
            "password": password
        });
        $.ajax({
            method: "POST",
            url: "/moera-node/credentials",
            contentType: "application/json",
            data: data
        })
            .done(function () {
                alertSuccess("Credentials created successfully");
                $("#create").css("display", "none");
                $("#update").css("display", "block");
            })
            .fail(function (jqxhr) {
                alertFailure(jqxhr.responseJSON.message);
            });
}

function updateCredentials(token, login, password) {
        let data = JSON.stringify({
            "login": login,
            "password": password
        });
        $.ajax({
            method: "PUT",
            url: "/moera-node/credentials?token=" + encodeURIComponent(token),
            contentType: "application/json",
            data: data
        })
            .done(function () {
                alertSuccess("Credentials updated successfully");
                $("#update")[0].reset();
            })
            .fail(function (jqxhr) {
                alertFailure(jqxhr.responseJSON.message);
            });
}

function loginAndUpdateCredentials(login, password, newPassword) {
        let data = JSON.stringify({
            "login": login,
            "password": password
        });
        $.ajax({
            method: "POST",
            url: "/moera-node/tokens",
            contentType: "application/json",
            data: data
        })
            .done(function (response) {
                updateCredentials(response.token, login, newPassword);
            })
            .fail(function (jqxhr) {
                alertFailure(jqxhr.responseJSON.message);
            });
}

function initCredentials() {
    $.getJSON("/moera-node/credentials", function (data) {
        if (data.created) {
            $("#update").css("display", "block");
        } else {
            $("#create").css("display", "block");
        }
    })
        .fail(function () {
            alertFailure("Cannot access server");
        });
    $("#create-submit").click(function (event) {
        event.preventDefault();
        if ($("#password").val() != $("#confirm-password").val()) {
            alertFailure("Passwords are different");
            return;
        }
        createCredentials($("#login").val(), $("#password").val());
    });
    $("#update-submit").click(function (event) {
        event.preventDefault();
        if ($("#new-password").val() != $("#confirm-new-password").val()) {
            alertFailure("Passwords are different");
            return;
        }
        loginAndUpdateCredentials($("#old-login").val(), $("#old-password").val(), $("#new-password").val());
    });
}

function init() {
    switch (window.pageName) {
        case "index":
            initIndex();
            break;
        case "credentials":
            initCredentials();
            break;
    }
}

$(init);