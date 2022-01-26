///*
// * The MIT License (MIT)
// *
// * Copyright (c) 2019 Code Technology Studio
// *
// * Permission is hereby granted, free of charge, to any person obtaining a copy of
// * this software and associated documentation files (the "Software"), to deal in
// * the Software without restriction, including without limitation the rights to
// * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
// * the Software, and to permit persons to whom the Software is furnished to do so,
// * subject to the following conditions:
// *
// * The above copyright notice and this permission notice shall be included in all
// * copies or substantial portions of the Software.
// *
// * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
// * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
// * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
// * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
// * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// */
//package io.jpom.build;
//
//import cn.hutool.core.collection.CollUtil;
//import cn.hutool.core.date.SystemClock;
//import cn.hutool.core.io.FileUtil;
//import cn.hutool.core.io.IoUtil;
//import cn.hutool.core.io.LineHandler;
//import cn.hutool.core.io.file.FileCopier;
//import cn.hutool.core.lang.Tuple;
//import cn.hutool.core.text.CharSequenceUtil;
//import cn.hutool.core.thread.ThreadUtil;
//import cn.hutool.core.util.EnumUtil;
//import cn.hutool.core.util.IdUtil;
//import cn.hutool.core.util.StrUtil;
//import cn.jiangzeyin.common.DefaultSystemLog;
//import cn.jiangzeyin.common.spring.SpringUtil;
//import io.jpom.common.BaseServerController;
//import io.jpom.model.data.BuildInfoModel;
//import io.jpom.model.data.RepositoryModel;
//import io.jpom.model.data.UserModel;
//import io.jpom.model.enums.BuildReleaseMethod;
//import io.jpom.model.enums.BuildStatus;
//import io.jpom.model.enums.GitProtocolEnum;
//import io.jpom.model.log.BuildHistoryLog;
//import io.jpom.plugin.DefaultPlugin;
//import io.jpom.plugin.IPlugin;
//import io.jpom.plugin.PluginFactory;
//import io.jpom.service.dblog.BuildInfoService;
//import io.jpom.service.dblog.DbBuildHistoryLogService;
//import io.jpom.system.ExtConfigBean;
//import io.jpom.system.JpomRuntimeException;
//import io.jpom.util.CommandUtil;
//import io.jpom.util.GitUtil;
//import io.jpom.util.StringUtil;
//import org.springframework.util.AntPathMatcher;
//import org.springframework.util.Assert;
//
//import java.io.File;
//import java.io.IOException;
//import java.io.InputStream;
//import java.nio.file.FileVisitResult;
//import java.nio.file.Path;
//import java.nio.file.SimpleFileVisitor;
//import java.nio.file.attribute.BasicFileAttributes;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.TimeUnit;
//import java.util.function.Supplier;
//
///**
// * new build info manage runnable
// *
// * @author Hotstrip
// * @since 20210-08-23
// */
