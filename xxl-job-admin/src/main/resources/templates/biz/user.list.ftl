<!DOCTYPE html>
<html>
<head>
	<#-- import macro -->
	<#import "../common/common.macro.ftl" as netCommon>

	<!-- 1-style start -->
	<@netCommon.commonStyle />
	<link rel="stylesheet" href="${request.contextPath}/static/plugins/bootstrap-table/bootstrap-table.min.css">
	<link rel="stylesheet" href="${request.contextPath}/static/adminlte/plugins/iCheck/square/blue.css">
	<!-- 1-style end -->

</head>
<body class="hold-transition" style="background-color: #ecf0f5;">
<div class="wrapper">
	<section class="content">

		<!-- 2-content start -->

		<#-- 查询区域：角色、账号 -->
		<div class="box" style="margin-bottom:9px;">
			<div class="box-body">
				<div class="row" id="data_filter" >

					<div class="col-xs-3">
						<div class="input-group">
							<span class="input-group-addon">${I18n.user_role}</span>
							<select class="form-control" id="role" >
								<option value="-1" >${I18n.system_all}</option>
								<#if roleList?exists && roleList?size gt 0>
									<#list roleList as role>
										<option value="${role.id}" >${role.name}</option>
									</#list>
								</#if>
							</select>
						</div>
					</div>
					<div class="col-xs-3">
						<div class="input-group">
							<span class="input-group-addon">${I18n.user_username}</span>
							<input type="text" class="form-control" id="username" autocomplete="on" >
						</div>
					</div>

					<div class="col-xs-1">
						<button class="btn btn-block btn-primary searchBtn" >${I18n.system_search}</button>
					</div>
					<div class="col-xs-1">
						<button class="btn btn-block btn-default resetBtn" >${I18n.system_reset}</button>
					</div>
				</div>
			</div>
		</div>

		<#-- 数据表格区域 -->
		<div class="row">
			<div class="col-xs-12">
				<div class="box">
					<div class="box-header pull-left" id="data_operation" >
						<button class="btn btn-sm btn-info add" type="button"><i class="fa fa-plus" ></i>${I18n.system_opt_add}</button>
						<button class="btn btn-sm btn-warning selectOnlyOne update" type="button"><i class="fa fa-edit"></i>${I18n.system_opt_edit}</button>
						<button class="btn btn-sm btn-danger selectAny delete" type="button"><i class="fa fa-remove "></i>${I18n.system_opt_del}</button>
					</div>
					<div class="box-body" >
						<table id="data_list" class="table table-bordered table-striped" width="100%" >
							<thead></thead>
							<tbody></tbody>
							<tfoot></tfoot>
						</table>
					</div>
				</div>
			</div>
		</div>

		<!-- 新增.模态框：账号、密码、角色、权限 -->
		<div class="modal fade" id="addModal" tabindex="-1" role="dialog"  aria-hidden="true">
			<div class="modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						<h4 class="modal-title" >${I18n.user_add}</h4>
					</div>
					<div class="modal-body">
						<form class="form-horizontal form" role="form" >
							<div class="form-group">
								<label for="lastname" class="col-sm-2 control-label">${I18n.user_username}<font color="red">*</font></label>
								<div class="col-sm-8"><input type="text" class="form-control" name="username" placeholder="${I18n.system_please_input}${I18n.user_username}" maxlength="20" ></div>
							</div>
							<div class="form-group">
								<label for="lastname" class="col-sm-2 control-label">${I18n.user_password}<font color="red">*</font></label>
								<div class="col-sm-8"><input type="text" class="form-control" name="password" placeholder="${I18n.system_please_input}${I18n.user_password}" maxlength="20" ></div>
							</div>
							<div class="form-group">
								<label for="lastname" class="col-sm-2 control-label">${I18n.user_role}<font color="red">*</font></label>
								<div class="col-sm-8">
									<select class="form-control" name="role" >
										<#if roleList?exists && roleList?size gt 0>
											<#list roleList as role>
												<option value="${role.id}" >${role.name}</option>
											</#list>
										</#if>
									</select>
								</div>
							</div>
							<div class="form-group">
								<label for="lastname" class="col-sm-2 control-label">${I18n.user_permission}<font color="black">*</font></label>
								<div class="col-sm-10" id="addPermissionGroup">
									<#if groupList?exists && groupList?size gt 0>
										<#list groupList as item>
											<input type="checkbox" name="permission" value="${item.id}" />&nbsp;&nbsp;${item.title}：${item.appname}
											<br>
										</#list>
									</#if>
								</div>
							</div>

							<hr>
							<div class="form-group">
								<div class="col-sm-offset-3 col-sm-6">
									<button type="submit" class="btn btn-primary"  >${I18n.system_save}</button>
									<button type="button" class="btn btn-default" data-dismiss="modal">${I18n.system_cancel}</button>
								</div>
							</div>

						</form>
					</div>
				</div>
			</div>
		</div>

		<!-- 更新.模态框：账号、密码、角色、权限 -->
		<div class="modal fade" id="updateModal" tabindex="-1" role="dialog"  aria-hidden="true">
			<div class="modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						<h4 class="modal-title" >${I18n.user_update}</h4>
					</div>
					<div class="modal-body">
						<form class="form-horizontal form" role="form" >
							<div class="form-group">
								<label for="lastname" class="col-sm-2 control-label">${I18n.user_username}<font color="red">*</font></label>
								<div class="col-sm-8"><input type="text" class="form-control" name="username" placeholder="${I18n.system_please_input}${I18n.user_username}" maxlength="20" readonly ></div>
							</div>
							<div class="form-group">
								<label for="lastname" class="col-sm-2 control-label">${I18n.user_password}<font color="red">*</font></label>
								<div class="col-sm-8"><input type="text" class="form-control" name="password" placeholder="${I18n.user_password_update_placeholder}" maxlength="20" ></div>
							</div>
							<div class="form-group">
								<label for="lastname" class="col-sm-2 control-label">${I18n.user_role}<font color="red">*</font></label>
								<div class="col-sm-8">
									<select class="form-control" name="role" >
										<#if roleList?exists && roleList?size gt 0>
											<#list roleList as role>
												<option value="${role.id}" >${role.name}</option>
											</#list>
										</#if>
									</select>
								</div>
							</div>
							<div class="form-group">
								<label for="lastname" class="col-sm-2 control-label">${I18n.user_permission}<font color="black">*</font></label>
								<div class="col-sm-10" id="updatePermissionGroup">
									<#if groupList?exists && groupList?size gt 0>
										<#list groupList as item>
											<input type="checkbox" name="permission" value="${item.id}" />${item.title}(${item.appname})<br>
										</#list>
									</#if>
								</div>
							</div>

							<hr>
							<div class="form-group">
								<div class="col-sm-offset-3 col-sm-6">
									<button type="submit" class="btn btn-primary"  >${I18n.system_save}</button>
									<button type="button" class="btn btn-default" data-dismiss="modal">${I18n.system_cancel}</button>
									<input type="hidden" name="id" >
								</div>
							</div>

						</form>
					</div>
				</div>
			</div>
		</div>

		<!-- 2-content end -->

	</section>
</div>

<!-- 3-script start -->
<@netCommon.commonScript />
<script src="${request.contextPath}/static/plugins/bootstrap-table/bootstrap-table.min.js"></script>
<script src="${request.contextPath}/static/plugins/bootstrap-table/locale/<#if I18n.admin_i18n?? && I18n.admin_i18n == 'en'>bootstrap-table-en-US.min.js<#else>bootstrap-table-zh-CN.min.js</#if>"></script>
<script src="${request.contextPath}/static/adminlte/plugins/iCheck/icheck.min.js"></script>
<#-- admin table -->
<script src="${request.contextPath}/static/biz/common/admin.table.js"></script>
<script>
	$(function() {

		<#-- 构建角色映射：key=角色ID，value=角色名称 -->
		<#-- 关联逻辑：xxl_job_user.role 关联 xxl_job_role.id -->
		var roleMap = {};
		<#if roleList?exists && roleList?size gt 0>
			<#list roleList as role>
				roleMap["${role.id}"] = "${role.name}";
			</#list>
		</#if>

		/**
		 * 初始化数据表格
		 * 展示用户列表：账号、密码、角色
		 * 
		 * 关联说明：
		 * - 用户表 `xxl_job_user.role` 字段关联角色表 `xxl_job_role.id`
		 */
		$.adminTable.initTable({
			table: '#data_list',
			url: base_url + "/user/pageList",
			queryParams: function (params) {
				var obj = {};
				obj.username = $('#username').val();
				obj.role = $('#role').val();
				obj.offset = params.offset;
				obj.pagesize = params.limit;
				return obj;
			},
			columns:[
				{
					checkbox: true,
					field: 'state',
					width: '5',
					widthUnit: '%',
					align: 'center',
					valign: 'middle'
				},{
					title: I18n.user_username,
					field: 'username',
					width: '20',
					widthUnit: '%',
					align: 'left'
				},{
					title: I18n.user_password,
					field: 'password',
					width: '20',
					widthUnit: '%',
					formatter: function(value, row, index) {
						return '******';
					}
				},{
					title: I18n.user_role,
					field: 'role',
					width: '10',
					widthUnit: '%',
					formatter: function(value, row, index) {
						// 通过角色ID获取角色名称
						var roleName = roleMap[value + ""];
						return roleName ? roleName : value;
					}
				}
			]
		});

		/**
		 * 初始化删除操作
		 */
		$.adminTable.initDelete({
			url: base_url + "/user/delete"
		});

		/**
		 * 初始化新增操作
		 * 表单验证：账号格式、密码长度
		 */
		// 账号验证方法：小写字母开头，由小写字母和数字组成
		jQuery.validator.addMethod("myValid01", function(value, element) {
			var length = value.length;
			var valid = /^[a-z][a-z0-9]*$/;
			return this.optional(element) || valid.test(value);
		}, I18n.user_username_valid );
		
		$.adminTable.initAdd( {
			url: base_url + "/user/insert",
			rules : {
				username : {
					required : true,
					rangelength:[4, 20],
					myValid01: true
				},
				password : {
					required : true,
					rangelength:[4, 20]
				}
			},
			messages : {
				username : {
					required : I18n.system_please_input + I18n.user_username,
					rangelength: I18n.system_lengh_limit + "[4-20]"
				},
				password : {
					required : I18n.system_please_input + I18n.user_password,
					rangelength: I18n.system_lengh_limit + "[4-20]"
				}
			},
			writeFormData: function() {
				// 打开新增窗口时无需特殊处理
			},
			readFormData: function() {
				// 读取表单数据
				return $("#addModal .form").serializeArray();
			}
		});

		/**
		 * 初始化更新操作
		 */
		$.adminTable.initUpdate( {
			url: base_url + "/user/update",
			writeFormData: function(row) {

				// 填充基础数据
				$("#updateModal .form input[name='id']").val( row.id );
				$("#updateModal .form input[name='username']").val( row.username );
				$("#updateModal .form input[name='password']").val( '' );
				// 选中用户当前绑定的角色
				$("#updateModal .form select[name='role']").val(row.role);

				// 回显用户已有的执行器权限
				var permissionArr = [];
				if (row.permission) {
					permissionArr = row.permission.split(",");
				}
				$("#updateModal .form input[name='permission']").each(function () {
					if($.inArray($(this).val(), permissionArr) > -1) {
						$(this).prop("checked",true);
					} else {
						$(this).prop("checked",false);
					}
				});

			},
			readFormData: function() {
				// 读取表单数据
				return $("#updateModal .form").serializeArray();
			}
		});

	});

</script>
<!-- 3-script end -->

</body>
</html>
