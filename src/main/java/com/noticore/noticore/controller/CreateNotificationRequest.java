package com.noticore.noticore.controller;

import com.noticore.noticore.model.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// A DTO (Data Transfer Object) is a plain class shaped exactly like the JSON
// you expect the client to send -- separate from the Notification @Entity.
// Why not just use Notification directly? Because the client shouldn't be
// able to set fields like `status` or `id` themselves in the request body --
// this class only exposes what a caller is actually allowed to provide.
public class CreateNotificationRequest {

    @NotBlank(message = "userId is required")
    public String userId;

    @NotNull(message = "channel is required")
    public ChannelType channel;

    @NotBlank(message = "recipient is required")
    public String recipient;

    @NotBlank(message = "message is required")
    public String message;
}
