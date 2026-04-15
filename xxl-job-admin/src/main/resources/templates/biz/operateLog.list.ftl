<!DOCTYPE html>
<html>
<head>
	<#-- import macro -->
	<#import "../common/common.macro.ftl" as netCommon>

	<!-- 1-style start -->
	<@netCommon.commonStyle />
	<link rel="stylesheet" href="${request.contextPath}/static/plugins/bootstrap-table/bootstrap-table.min.css">
	<link rel="stylesheet" href="${request.contextPath}/static/adminlte/bower_components/bootstrap-daterangepicker/daterangepicker.css">
	<!-- 1-style end -->

</head>
<body class="hold-transition" style="background-color: #ecf0f5;">
<div class="wrapper">
	<section class="content">

		<!-- 2-content start -->

		<#-- 查询区域 -->
		<div class="box" style="margin-bottom:9px;">
			<div class="box-body">
				<div class="row" id="data_filter" >

					<div class="col-xs-2">
						<div class="input-group">
							<span class="input-group-addon">${I18n.operate_log_module}</span>
							<select class="form-control" id="module"  >
								<option value="">${I18n.system_all}</option>
								<option value="LOGIN">${I18n.operate_log_module_login}</option>
								<option value="USER">${I18n.operate_log_module_user}</option>
								<option value="JOB_GROUP">${I18n.operate_log_module_jobgroup}</option>
								<option value="JOB_INFO">${I18n.operate_log_module_jobinfo}</option>
							</select>
						</div>
					</div>

					<div class="col-xs-2" id="actionContainer">
						<div class="input-group">
							<span class="input-group-addon">${I18n.operate_log_action}</span>
							<select class="form-control" id="action"  >
								<option value="">${I18n.system_all}</option>
							</select>
						</div>
					</div>

					<div class="col-xs-2">
						<div class="input-group">
							<span class="input-group-addon">${I18n.operate_log_operator}</span>
							<input type="text" class="form-control" id="operator" placeholder="${I18n.system_please_input}" >
						</div>
					</div>

					<div class="col-xs-2" id="jobGroupContainer">
						<div class="input-group">
							<span class="input-group-addon">${I18n.jobinfo_field_jobgroup}</span>
							<select class="form-control" id="jobGroup"  >
								<option value="">${I18n.system_all}</option>
								<#list groupList as group>
									<option value="${group.id}">${group.title}</option>
								</#list>
							</select>
						</div>
					</div>

					<div class="col-xs-4">
						<div class="input-group">
                		<span class="input-group-addon">
	                  		${I18n.joblog_field_triggerTime}
	                	</span>
							<input type="text" class="form-control" id="filterTime" readonly >
						</div>
					</div>

				</div>
				<div class="row" style="margin-top:10px;">
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

		<!-- 2-content end -->

	</section>
</div>

<!-- 3-script start -->
<@netCommon.commonScript />
<script src="${request.contextPath}/static/plugins/bootstrap-table/bootstrap-table.min.js"></script>
<script src="${request.contextPath}/static/plugins/bootstrap-table/locale/<#if I18n.admin_i18n?? && I18n.admin_i18n == 'en'>bootstrap-table-en-US.min.js<#else>bootstrap-table-zh-CN.min.js</#if>"></script>
<script src="${request.contextPath}/static/adminlte/bower_components/moment/moment.min.js"></script>
<script src="${request.contextPath}/static/adminlte/bower_components/bootstrap-daterangepicker/daterangepicker.js"></script>
<script src="${request.contextPath}/static/biz/common/admin.table.js"></script>
<script>
	$(function() {

		// ---------------------- module actions mapping ----------------------
		var moduleActions = {
			"": [],
			"LOGIN": [
				{value: "LOGIN", text: "${I18n.operate_log_action_login}"},
				{value: "LOGOUT", text: "${I18n.operate_log_action_logout}"}
			],
			"USER": [
				{value: "ADD", text: "${I18n.operate_log_action_add}"},
				{value: "UPDATE", text: "${I18n.operate_log_action_update}"},
				{value: "DELETE", text: "${I18n.operate_log_action_delete}"}
			],
			"JOB_GROUP": [
				{value: "ADD", text: "${I18n.operate_log_action_add}"},
				{value: "UPDATE", text: "${I18n.operate_log_action_update}"},
				{value: "DELETE", text: "${I18n.operate_log_action_delete}"}
			],
			"JOB_INFO": [
				{value: "ADD", text: "${I18n.operate_log_action_add}"},
				{value: "UPDATE", text: "${I18n.operate_log_action_update}"},
				{value: "DELETE", text: "${I18n.operate_log_action_delete}"},
				{value: "START", text: "${I18n.operate_log_action_start}"},
				{value: "STOP", text: "${I18n.operate_log_action_stop}"}
			]
		};

		// ---------------------- module change ----------------------
		$('#module').on('change', function(){
			var module = $(this).val();
			var actions = moduleActions[module] || [];

			var actionHtml = '<option value="">${I18n.system_all}</option>';
			for (var i = 0; i < actions.length; i++) {
				actionHtml += '<option value="' + actions[i].value + '">' + actions[i].text + '</option>';
			}
			$('#action').html(actionHtml);

			if (module == 'JOB_GROUP' || module == 'JOB_INFO') {
				$('#jobGroupContainer').show();
			} else {
				$('#jobGroupContainer').hide();
				$('#jobGroup').val('');
			}

			if (module == 'LOGIN') {
				$('#actionContainer').show();
			} else if (module == '') {
				$('#actionContainer').show();
			} else {
				$('#actionContainer').show();
			}
		});

		// ---------------------- filter Time ----------------------
		var rangesConf = {};
		rangesConf[I18n.daterangepicker_ranges_today] = [moment().startOf('day'), moment().endOf('day')];
		rangesConf[I18n.daterangepicker_ranges_yesterday] = [moment().subtract(1, 'days').startOf('day'), moment().subtract(1, 'days').endOf('day')];
		rangesConf[I18n.daterangepicker_ranges_this_month] = [moment().startOf('month'), moment().endOf('month')];
		rangesConf[I18n.daterangepicker_ranges_last_month] = [moment().subtract(1, 'months').startOf('month'), moment().subtract(1, 'months').endOf('month')];
		rangesConf[I18n.daterangepicker_ranges_recent_week] = [moment().subtract(1, 'weeks').startOf('day'), moment().endOf('day')];
		rangesConf[I18n.daterangepicker_ranges_recent_month] = [moment().subtract(1, 'months').startOf('day'), moment().endOf('day')];

		$('#filterTime').daterangepicker({
			autoApply:false,
			singleDatePicker:false,
			showDropdowns:true,
			timePicker: true,
			timePicker24Hour: true,
			timePickerSeconds: true,
			opens : 'left',
			ranges: rangesConf,
			locale : {
				format: 'YYYY-MM-DD HH:mm:ss',
				separator : ' - ',
				customRangeLabel : I18n.daterangepicker_custom_name ,
				applyLabel : I18n.system_ok ,
				cancelLabel : I18n.system_cancel ,
				fromLabel : I18n.daterangepicker_custom_starttime ,
				toLabel : I18n.daterangepicker_custom_endtime ,
				daysOfWeek : I18n.daterangepicker_custom_daysofweek.split(',') ,
				monthNames : I18n.daterangepicker_custom_monthnames.split(',') ,
				firstDay : 1
			}
		});

		// init filter
		function resetFilter(){
			$('#filterTime').data("daterangepicker").setStartDate( rangesConf[I18n.daterangepicker_ranges_recent_week][0] );
			$('#filterTime').data("daterangepicker").setEndDate( rangesConf[I18n.daterangepicker_ranges_recent_week][1] );

			$("#module").val('');
			$("#action").val('');
			$("#operator").val('');
			$("#jobGroup").val('');
			$('#jobGroupContainer').show();
			$('#actionContainer').show();
		}
		resetFilter();

		// ---------------------- page ----------------------

		/**
		 * init table
		 */
		$.adminTable.initTable({
			table: '#data_list',
			url: base_url + "/operateLog/pageList",
			queryParams: function (params) {
				var obj = {};
				obj.module = $('#module').val();
				obj.action = $('#action').val();
				obj.operator = $('#operator').val();
				obj.filterTime = $('#filterTime').val();
				var jobGroupVal = $('#jobGroup').val();
				if (jobGroupVal != '') {
					obj.jobGroup = parseInt(jobGroupVal);
				}
				obj.offset = params.offset;
				obj.pagesize = params.limit;
				return obj;
			},
			resetHandler : function() {
				resetFilter();
			},
			columns:[
				{
					title: 'ID',
					field: 'id',
					width: '5',
					widthUnit: '%',
					align: 'center'
				},
				{
					title: "${I18n.operate_log_module}",
					field: 'module',
					width: '10',
					widthUnit: '%',
					align: 'left',
					formatter: function(value, row, index) {
						var text = value;
						if (value == 'LOGIN') text = "${I18n.operate_log_module_login}";
						else if (value == 'USER') text = "${I18n.operate_log_module_user}";
						else if (value == 'JOB_GROUP') text = "${I18n.operate_log_module_jobgroup}";
						else if (value == 'JOB_INFO') text = "${I18n.operate_log_module_jobinfo}";
						return text;
					}
				},
				{
					title: "${I18n.operate_log_action}",
					field: 'action',
					width: '8',
					widthUnit: '%',
					align: 'left',
					formatter: function(value, row, index) {
						var text = value;
						if (value == 'ADD') text = "${I18n.operate_log_action_add}";
						else if (value == 'UPDATE') text = "${I18n.operate_log_action_update}";
						else if (value == 'DELETE') text = "${I18n.operate_log_action_delete}";
						else if (value == 'START') text = "${I18n.operate_log_action_start}";
						else if (value == 'STOP') text = "${I18n.operate_log_action_stop}";
						else if (value == 'LOGIN') text = "${I18n.operate_log_action_login}";
						else if (value == 'LOGOUT') text = "${I18n.operate_log_action_logout}";
						return text;
					}
				},
				{
					title: "${I18n.operate_log_operator}",
					field: 'operator',
					width: '10',
					widthUnit: '%',
					align: 'left'
				},
				{
					title: "${I18n.operate_log_operate_time}",
					field: 'operateTime',
					width: '15',
					widthUnit: '%',
					formatter: function(value, row, index) {
						return value?moment(value).format("YYYY-MM-DD HH:mm:ss"):"";
					}
				},
				{
					title: "${I18n.operate_log_ip}",
					field: 'ip',
					width: '10',
					widthUnit: '%',
					align: 'left'
				},
				{
					title: "${I18n.jobinfo_field_jobgroup}",
					field: 'groupName',
					width: '10',
					widthUnit: '%',
					align: 'left',
					formatter: function(value, row, index) {
						if (value) {
							return value;
						}
						if (row.module == 'JOB_GROUP' && row.targetName) {
							return row.targetName;
						}
						return "-";
					}
				},
				{
					title: "${I18n.jobinfo_field_jobdesc}",
					field: 'jobDesc',
					width: '12',
					widthUnit: '%',
					align: 'left',
					formatter: function(value, row, index) {
						if (value) {
							return value.length > 15 ? value.substr(0, 15) + '...' : value;
						}
						if (row.module == 'JOB_INFO' && row.targetName) {
							var desc = row.targetName;
							return desc.length > 15 ? desc.substr(0, 15) + '...' : desc;
						}
						if (row.module == 'USER' && row.targetName) {
							return row.targetName;
						}
						if (row.module == 'LOGIN' && row.targetName) {
							return row.targetName;
						}
						return "-";
					}
				},
				{
					title: "${I18n.operate_log_target_id}",
					field: 'targetId',
					width: '8',
					widthUnit: '%',
					align: 'center',
					formatter: function(value, row, index) {
						return value ? value : "-";
					}
				}
			]
		});

	});
</script>
<!-- 3-script end -->

</body>
</html>
