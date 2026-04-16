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

		<#-- 查询区域：名称、编码、状态 -->
		<div class="box" style="margin-bottom:9px;">
			<div class="box-body">
				<div class="row" id="data_filter" >

					<div class="col-xs-3">
						<div class="input-group">
							<span class="input-group-addon">${I18n.role_name}</span>
							<input type="text" class="form-control" id="name" autocomplete="on" >
						</div>
					</div>
					<div class="col-xs-3">
						<div class="input-group">
							<span class="input-group-addon">${I18n.role_code}</span>
							<input type="text" class="form-control" id="code" autocomplete="on" >
						</div>
					</div>
					<div class="col-xs-3">
						<div class="input-group">
							<span class="input-group-addon">${I18n.system_status}</span>
							<select class="form-control" id="status" >
								<option value="-1" >${I18n.system_all}</option>
								<option value="1" >${I18n.role_status_enabled}</option>
								<option value="0" >${I18n.role_status_disabled}</option>
							</select>
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
						<button class="btn btn-sm btn-success selectAny enable" type="button"><i class="fa fa-play"></i>${I18n.role_opt_enable}</button>
						<button class="btn btn-sm btn-default selectAny disable" type="button"><i class="fa fa-pause"></i>${I18n.role_opt_disable}</button>
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

		<!-- 新增.模态框：表单只包含名称、编码 -->
		<div class="modal fade" id="addModal" tabindex="-1" role="dialog"  aria-hidden="true">
			<div class="modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						<h4 class="modal-title" >${I18n.role_add}</h4>
					</div>
					<div class="modal-body">
						<form class="form-horizontal form" role="form" >
							<div class="form-group">
								<label for="lastname" class="col-sm-2 control-label">${I18n.role_name}<font color="red">*</font></label>
								<div class="col-sm-8"><input type="text" class="form-control" name="name" placeholder="${I18n.system_please_input}${I18n.role_name}" maxlength="50" ></div>
							</div>
							<div class="form-group">
								<label for="lastname" class="col-sm-2 control-label">${I18n.role_code}<font color="red">*</font></label>
								<div class="col-sm-8"><input type="text" class="form-control" name="code" placeholder="${I18n.system_please_input}${I18n.role_code}" maxlength="50" ></div>
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

		<!-- 更新.模态框：表单只包含名称、编码（状态不能编辑） -->
		<div class="modal fade" id="updateModal" tabindex="-1" role="dialog"  aria-hidden="true">
			<div class="modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						<h4 class="modal-title" >${I18n.role_update}</h4>
					</div>
					<div class="modal-body">
						<form class="form-horizontal form" role="form" >
							<div class="form-group">
								<label for="lastname" class="col-sm-2 control-label">${I18n.role_name}<font color="red">*</font></label>
								<div class="col-sm-8"><input type="text" class="form-control" name="name" placeholder="${I18n.system_please_input}${I18n.role_name}" maxlength="50" ></div>
							</div>
							<div class="form-group">
								<label for="lastname" class="col-sm-2 control-label">${I18n.role_code}<font color="red">*</font></label>
								<div class="col-sm-8"><input type="text" class="form-control" name="code" placeholder="${I18n.system_please_input}${I18n.role_code}" maxlength="50" ></div>
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

		/**
		 * 初始化数据表格
		 * 展示角色列表：名称、编码、状态
		 */
		$.adminTable.initTable({
			table: '#data_list',
			url: base_url + "/role/pageList",
			queryParams: function (params) {
				var obj = {};
				obj.name = $('#name').val();
				obj.code = $('#code').val();
				obj.status = $('#status').val();
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
					title: I18n.role_name,
					field: 'name',
					width: '30',
					widthUnit: '%',
					align: 'left'
				},{
					title: I18n.role_code,
					field: 'code',
					width: '30',
					widthUnit: '%',
					align: 'left'
				},{
					title: I18n.system_status,
					field: 'status',
					width: '20',
					widthUnit: '%',
					formatter: function(value, row, index) {
						return value == 1 ? I18n.role_status_enabled : I18n.role_status_disabled;
					}
				}
			]
		});

		/**
		 * 初始化删除操作
		 */
		$.adminTable.initDelete({
			url: base_url + "/role/delete"
		});

		/**
		 * 初始化启用操作
		 * 将选中的角色状态改为启用
		 */
		$("#data_operation").on('click', '.enable',function() {
			var rows = $.adminTable.selectRows();
			if (rows.length <= 0) {
				layer.msg(I18n.system_please_choose + I18n.system_data);
				return;
			}

			layer.confirm(I18n.role_confirm_enable + '?', {
				icon: 3,
				title: I18n.system_tips ,
				btn: [ I18n.system_ok, I18n.system_cancel ]
			}, function(index){
				layer.close(index);

				$.ajax({
					type : 'POST',
					url : base_url + "/role/enable",
					data : {
						"ids" : $.adminTable.selectIds()
					},
					dataType : "json",
					success : function(data){
						if (data.code === 200) {
							layer.msg( I18n.role_opt_enable + I18n.system_success );
							$('#data_filter .searchBtn').click();
						} else {
							layer.msg( data.msg || I18n.role_opt_enable + I18n.system_fail );
						}
					}
				});
			});
		});

		/**
		 * 初始化禁用操作
		 * 将选中的角色状态改为禁用
		 */
		$("#data_operation").on('click', '.disable',function() {
			var rows = $.adminTable.selectRows();
			if (rows.length <= 0) {
				layer.msg(I18n.system_please_choose + I18n.system_data);
				return;
			}

			layer.confirm(I18n.role_confirm_disable + '?', {
				icon: 3,
				title: I18n.system_tips ,
				btn: [ I18n.system_ok, I18n.system_cancel ]
			}, function(index){
				layer.close(index);

				$.ajax({
					type : 'POST',
					url : base_url + "/role/disable",
					data : {
						"ids" : $.adminTable.selectIds()
					},
					dataType : "json",
					success : function(data){
						if (data.code === 200) {
							layer.msg( I18n.role_opt_disable + I18n.system_success );
							$('#data_filter .searchBtn').click();
						} else {
							layer.msg( data.msg || I18n.role_opt_disable + I18n.system_fail );
						}
					}
				});
			});
		});

		/**
		 * 初始化新增操作
		 * 表单验证：名称、编码必填，长度2-50
		 */
		$.adminTable.initAdd( {
			url: base_url + "/role/insert",
			rules : {
				name : {
					required : true,
					rangelength:[2, 50]
				},
				code : {
					required : true,
					rangelength:[2, 50]
				}
			},
			messages : {
				name : {
					required : I18n.system_please_input + I18n.role_name,
					rangelength: I18n.system_lengh_limit + "[2-50]"
				},
				code : {
					required : I18n.system_please_input + I18n.role_code,
					rangelength: I18n.system_lengh_limit + "[2-50]"
				}
			},
			writeFormData: function() {
				// 打开新增窗口时无需特殊处理
			},
			readFormData: function() {
				return $("#addModal .form").serializeArray();
			}
		});

		/**
		 * 初始化更新操作
		 * 注意：禁用状态的角色不能编辑
		 * 编辑时不能修改状态，状态只能通过启用/禁用按钮操作
		 */
		$.adminTable.initUpdate( {
			url: base_url + "/role/update",
			writeFormData: function(row) {

				// 禁用状态的角色不能编辑
				if (row.status == 0) {
					$('#updateModal').modal('hide');
					layer.msg(I18n.role_disabled_cannot_edit);
					return;
				}

				// 填充表单数据（状态不能编辑）
				$("#updateModal .form input[name='id']").val( row.id );
				$("#updateModal .form input[name='name']").val( row.name );
				$("#updateModal .form input[name='code']").val( row.code );

			},
			readFormData: function() {
				return $("#updateModal .form").serializeArray();
			}
		});

	});

</script>
<!-- 3-script end -->

</body>
</html>
