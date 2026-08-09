package org.moera.node.rest.notification;

import java.util.List;
import jakarta.inject.Inject;

import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.SheriffMark;
import org.moera.lib.node.types.SubscriptionReason;
import org.moera.node.data.Contact;
import org.moera.node.liberin.model.MentionInRemotePostingAddedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.operations.ContactOperations;
import org.moera.node.operations.SubscriptionOperations;
import org.moera.node.task.Job;
import tools.jackson.databind.ObjectMapper;

public class MentionPostingAddedJob extends Job<MentionPostingAddedJob.Parameters, Object> {

    public record Parameters(
        String senderNodeName,
        String postingId,
        String ownerName,
        String ownerFullName,
        String ownerGender,
        AvatarImage ownerAvatar,
        String heading,
        List<String> sheriffs,
        List<SheriffMark> sheriffMarks
    ) {
    }

    @Inject
    private SubscriptionOperations subscriptionOperations;

    @Inject
    private MediaManager mediaManager;

    @Inject
    private ContactOperations contactOperations;

    public MentionPostingAddedJob() {
    }

    @Override
    protected void setParameters(String parameters, ObjectMapper objectMapper) {
        this.parameters = objectMapper.readValue(parameters, Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) {
        this.state = null;
    }

    @Override
    protected void execute() throws Exception {
        Contact.toAvatar(
            contactOperations.find(parameters.ownerName),
            parameters.ownerAvatar
        );

        tx.executeWriteWithExceptions(() ->
            mediaManager.downloadAvatar(parameters.senderNodeName, parameters.ownerAvatar)
        );

        universalContext.send(
            new MentionInRemotePostingAddedLiberin(
                parameters.senderNodeName,
                parameters.ownerName,
                parameters.ownerFullName,
                parameters.ownerGender,
                parameters.ownerAvatar,
                parameters.postingId,
                parameters.heading,
                parameters.sheriffs,
                parameters.sheriffMarks
            )
        );

        tx.executeWrite(() ->
            subscriptionOperations.subscribeToPostingComments(
                parameters.senderNodeName,
                parameters.postingId,
                SubscriptionReason.MENTION
            )
        );
    }

}
