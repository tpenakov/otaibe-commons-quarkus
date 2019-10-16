package org.otaibe.commons.quarkus.actuator.web.domain;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@RegisterForReflection
public class GitInfo {

    public static final String GIT_BRANCH = "git.branch";
    public static final String GIT_COMMIT_ID = "git.commit.id";
    public static final String GIT_COMMIT_TIME = "git.commit.time";

    String branch;
    CommitInfo commit;

    @Data
    @NoArgsConstructor
    @RegisterForReflection
    public static class CommitInfo {
        String time;
        String id;
    }
}
