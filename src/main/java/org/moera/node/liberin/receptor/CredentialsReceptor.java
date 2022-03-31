package org.moera.node.liberin.receptor;

import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.PasswordResetLiberin;
import org.moera.node.mail.PasswordResetMail;

@LiberinReceptor
public class CredentialsReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void passwordReset(PasswordResetLiberin liberin) {
        send(liberin, new PasswordResetMail(liberin.getToken()));
    }

}
