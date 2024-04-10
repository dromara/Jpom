//package org.dromara.jpom.build.pipeline.actuator;
//
//import org.dromara.jpom.build.pipeline.model.config.Repository;
//import org.springframework.util.Assert;
//
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * @author bwcx_jzy
// * @since 2024/4/10
// */
//public class RepositoryActuator2 {
//
//    private final Map<String, Repository> repositoryMap;
//
//    private final Map<String, Boolean> pullCache;
//
//    public RepositoryActuator2(Map<String, Repository> repositoryMap) {
//        this.repositoryMap = repositoryMap;
//        this.pullCache = new HashMap<>(repositoryMap.size());
//    }
//
//    public void pull(String tag) {
//        Repository repository = repositoryMap.get(tag);
//        Assert.notNull(repository, "没有找到对应标记的仓库");
//        if (pullCache.containsKey(tag)) {
//            return;
//        }
//    }
//}
