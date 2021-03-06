/**
 * Licensed to the Austrian Association for Software Tool Integration (AASTI)
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. The AASTI licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openengsb.connector.userprojects.file.internal.file;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openengsb.connector.userprojects.file.internal.Configuration;
import org.openengsb.domain.userprojects.model.User;

/**
 * The object providing access to the users file.
 */
public class UserFileAccessObject extends BaseFileAccessObject {

    private final File usersFile;

    public UserFileAccessObject() {
        usersFile = Configuration.get().getUsersFile();
    }

    /**
     * Finds all the available users.
     * 
     * @return the list of available users
     */
    public List<User> findAllUsers() {
        List<User> list = new ArrayList<>();
        List<String> usernames;
        try {
            usernames = readLines(usersFile);
        } catch (IOException e) {
            throw new FileBasedRuntimeException(e);
        }
        for (String username : usernames) {
            list.add(new User(username));
        }
        
        return list;
    }

}
