/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.unomi.graphql.commands.list;

import org.apache.unomi.api.Event;
import org.apache.unomi.api.Profile;
import org.apache.unomi.api.services.EventService;
import org.apache.unomi.api.services.ProfileService;
import org.apache.unomi.graphql.CDPGraphQLConstants;
import org.apache.unomi.graphql.commands.BaseCommand;
import org.apache.unomi.graphql.converters.UserListConverter;
import org.apache.unomi.graphql.types.input.CDPProfileIDInput;
import org.apache.unomi.graphql.types.output.CDPList;
import org.apache.unomi.graphql.utils.EventBuilder;
import org.apache.unomi.lists.UserList;
import org.apache.unomi.services.UserListService;

import java.util.Collections;
import java.util.Objects;

public class AddProfileToListCommand extends BaseCommand<CDPList> {

    private final String listId;
    private final CDPProfileIDInput profileIDInput;
    private final Boolean active;

    private AddProfileToListCommand(final Builder builder) {
        super(builder);

        this.listId = builder.listId;
        this.profileIDInput = builder.profileIDInput;
        this.active = builder.active;
    }

    @Override
    public CDPList execute() {
        final UserList userList = serviceManager.getService(UserListService.class).load(listId);

        if (userList == null) {
            return null;
        }

        ProfileService profileService = serviceManager.getService(ProfileService.class);
        final Profile profile = profileService.load(profileIDInput.getId());

        if (profile == null) {
            return null;
        }

        final Event event = EventBuilder.create(CDPGraphQLConstants.CDP_LIST_UPDATE_EVENT_NAME, profile)
                .setProperty("joinLists", Collections.singletonList(listId))
                .setPersistent(true)
                .build();

        if (serviceManager.getService(EventService.class).send(event) == EventService.PROFILE_UPDATED) {
            profileService.save(event.getProfile());
        }

        return new CDPList(UserListConverter.convertToUnomiList(userList));
    }

    public static Builder create() {
        return new Builder();
    }

    public static class Builder extends BaseCommand.Builder<Builder> {

        private String listId;
        private CDPProfileIDInput profileIDInput;
        private Boolean active;

        public Builder() {
        }

        public Builder listId(String listId) {
            this.listId = listId;
            return this;
        }

        public Builder profileIDInput(CDPProfileIDInput profileIDInput) {
            this.profileIDInput = profileIDInput;
            return this;
        }

        public Builder active(Boolean active) {
            this.active = active;
            return this;
        }

        @Override
        public void validate() {
            super.validate();

            Objects.requireNonNull(listId, "The \"listID\" argument can not be null");
            Objects.requireNonNull(profileIDInput, "The \"profileID\" argument can not be null");
        }

        public AddProfileToListCommand build() {
            validate();

            return new AddProfileToListCommand(this);
        }

    }

}
