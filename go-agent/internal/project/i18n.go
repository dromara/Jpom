/*
 * Copyright (c) 2019 Of Him Code Technology Studio
 * Jpom is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 * 			http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package project

import (
	"fmt"

	"jpom.io/go-agent/internal/i18n"
)

// DefaultLang 项目域非 HTTP 场景（WS/后台任务）使用的兜底语言。
const DefaultLang = "zh-CN"

// tr 按语言取 Java 端同 key 文案（占位符按 Java StrUtil.format 的 {} 顺序替换）。
func tr(lang, key string, args ...any) string {
	return i18n.T(lang, key, args...)
}

// trf 按语言取文案并用 fmt 语义替换 %s/%d 占位符（Java String.format 消息）。
func trf(lang, key string, args ...any) string {
	return fmt.Sprintf(i18n.T(lang, key), args...)
}

// normalizeLang 归一化语言标签（zh/zh_CN→zh-CN，en→en-US，未知→zh-CN）。
func normalizeLang(lang string) string {
	if lang == "" {
		return DefaultLang
	}
	return i18n.NormalizeLang(lang)
}

// i18n key 常量：与 Java modules/common/resources/i18n/messages_*.properties 完全一致。
const (
	keyServiceException         = "i18n.service_exception.3821"
	keyIncorrectProjectID       = "i18n.incorrect_project_id.5f70"
	keyProjectIDNotExist        = "i18n.project_id_does_not_exist.6b9b"
	keyNoProjectFound           = "i18n.no_project_found.ef5e"
	keyProjectNotExist          = "i18n.project_does_not_exist.3029"
	keyGetProjectInfoFailure    = "i18n.get_project_info_failure.ddff"
	keySelectOperationType      = "i18n.select_operation_type.63c6"
	keyMissingOperationParam    = "i18n.missing_operation_parameter.b16b"
	keyOperationNotSupported    = "i18n.current_operation_not_supported.3aec"
	keyOperationSucceeded       = "i18n.operation_succeeded.3313"
	keyOperationFailedDetails   = "i18n.operation_failed_with_details.7280"
	keyCheckPassed              = "i18n.check_passed.dce8"
	keyAddNewSuccess            = "i18n.add_new_success.431a"
	keyModifySuccess            = "i18n.modify_success.69be"
	keyDeletionSuccess          = "i18n.deletion_success_message.4359"
	keyDeleteSuccess            = "i18n.delete_success.0007"
	keyReleaseSuccessful        = "i18n.release_successful.f2ca"
	keySelectWorkspaceToModify  = "i18n.select_workspace_to_modify.ac87"
	keyProjectIDInUse           = "i18n.project_id_in_use.1adb"
	keyProjectAssociatedSpace   = "i18n.project_associated_with_other_workspaces_message.299c"
	keySelectRunMode            = "i18n.select_run_mode.5a5d"
	keyClassPathRequired        = "i18n.class_path_and_java_ext_dirs_cp_required.7557"
	keyJavaExtDirsCpRequired    = "i18n.java_ext_dirs_cp_required.1f4a"
	keyLinkIDRequired           = "i18n.link_id_required.5dc7"
	keySoftLinkNotExist         = "i18n.soft_link_project_does_not_exist.8ad2"
	keySoftLinkModeError        = "i18n.soft_link_project_mode_error.ffa0"
	keySelectProjectPath        = "i18n.select_correct_project_path_or_no_auth_configured.366a"
	keyProjectLogPathRequired   = "i18n.project_log_storage_path_required.d0bb"
	keyRestoreAuthorize         = "i18n.restore_authorization_data_exception.015a"
	keyInvalidProjectPath       = "i18n.invalid_project_path.04f7"
	keyProjectPathPromotion     = "i18n.project_path_promotion_issue.2250"
	keyProjectLogIsFolder       = "i18n.project_log_is_existing_folder.a80a"
	keyInvalidWebhooksAddress   = "i18n.invalid_webhooks_address.d836"
	keyConfigureDslContent      = "i18n.configure_dsl_content.42e3"
	keyRunStatusNotConfigured   = "i18n.run_status_not_configured.e959"
	keyProjectIDContainsSpace   = "i18n.project_id_cannot_contain_spaces.251d"
	keyProjectPathNoSpaces      = "i18n.project_path_no_spaces.263c"
	keyProjectPathIsFile        = "i18n.project_path_already_exists_as_file.a900"
	keyProjectPathConflict      = "i18n.project_path_conflict.8c6f"
	keyRunningCannotChangePath  = "i18n.running_project_cannot_change_path.5888"
	keyCannotDeleteRunning      = "i18n.cannot_delete_running_project.e56b"
	keyProjectSoftLinkedBy      = "i18n.project_soft_linked_by.8556"
	keyDeleteLogFileFailed      = "i18n.delete_log_file_failure_with_colon.d867"
	keyDeleteProjectFileFailed  = "i18n.delete_project_file_failure.f007"
	keyProgramAlreadyRunning    = "i18n.program_already_running.96e1"
	keyNoFilesInProjectDir      = "i18n.no_files_in_project_directory.108e"
	keyProjectTypeNotSupported  = "i18n.project_type_not_supported_for_startup.7bd1"
	keyNoCommandToExecute       = "i18n.no_command_to_execute.340b"
	keyFileTypeNoStop           = "i18n.file_type_no_stop.00ff"
	keyFileTypeNoRestart        = "i18n.file_type_no_restart.0977"
	keyFileTypeNoStatus         = "i18n.file_type_project_no_running_status.32a2"
	keyNonDslUnsupported        = "i18n.non_dsl_project_unsupported_operation.5c09"
	keyDslNotConfigured         = "i18n.dsl_not_configured.8a57"
	keyDslInfoNotConfigured     = "i18n.dsl_info_not_configured.3487"
	keyProcessNotExist          = "i18n.process_does_not_exist.4e39"
	keyScriptTemplateIDNeeded   = "i18n.script_template_id_required.f339"
	keyScriptLibraryNotExist    = "i18n.missing_script_library_message.be9a"
	keyScriptTemplateNotExist   = "i18n.script_template_not_exist.e05f"
	keyMainClassNotExist        = "i18n.main_class_attribute_not_found.93e8"
	keyMainClassNotFound        = "i18n.no_main_class_found.b001"
	keyMissingPackage           = "i18n.missing_package_in_root_dir.8bab"
	keyNoFileFound              = "i18n.no_file_found.7d40"
	keyCorrespondingFileNeeded  = "i18n.corresponding_file_required.57b3"
	keyNoCorrespondingFile      = "i18n.no_corresponding_file.97b5"
	keyNoCorrespondingFolder    = "i18n.no_corresponding_folder.621f"
	keyNoCorrespondingFileColon = "i18n.no_corresponding_file_colon.8970"
	keyLogFileNotFound          = "i18n.log_file_not_found.7f2e"
	keyDownloadFailedRetry      = "i18n.download_failed_retry.c113"
	keyResetSuccess             = "i18n.reset_success.faa3"
	keyResetFailed              = "i18n.reset_failed.5281"
	keyResetLogFailure          = "i18n.reset_log_failure.b3d3"
	keyStartExecuting           = "i18n.start_executing.f0b9"
	keyExecutionEndedDetail     = "i18n.execution_ended_with_detail.8f93"
	keyExitCode                 = "i18n.exit_code.3b54"
	keyAutoStartProjectFailed   = "i18n.auto_start_project_failed.c7b5"
	keyDataIDAlreadyExists      = "i18n.data_id_already_exists.28b6"
	keyDataNotExist             = "i18n.data_does_not_exist_with_details.d9b5"
	keyOperationFilePermission  = "i18n.operation_file_permission_exception.5a41"
	keyProjectIDNotFound        = "i18n.project_id_not_found.b87e"
	keyProjectIDLengthRange     = "i18n.project_id_length_range.7064"
	keyProjectIDKeywordUsed     = "i18n.project_id_keyword_occupied.1cae"
	keyCannotConfigureRootPath  = "i18n.cannot_configure_root_path.d86e"
	keyNeedAbsolutePath         = "i18n.need_configure_absolute_path.f2e6"
	keyProjectPathAuthJpom      = "i18n.project_path_auth_not_under_jpom.0e18"
	keyFileNotFound             = "i18n.file_not_found.d952"
	keySoftLinkTargetNotExist   = "i18n.soft_link_project_department_exists.fa97"
)
