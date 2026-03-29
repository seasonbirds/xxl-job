<!DOCTYPE html>
<html>
<head>
	<#-- import macro -->
	<#import "../common/common.macro.ftl" as netCommon>

	<!-- 1-style start -->
	<@netCommon.commonStyle />
	<link rel="stylesheet" href="${request.contextPath}/static/plugins/bootstrap-table/bootstrap-table.min.css">
	<!-- daterangepicker -->
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
							<span class="input-group-addon">${I18n.operation_module}</span>
							<select class="form-control" id="operationModule"  >
								<option value="" >${I18n.operation_module_all}</option>
								<#list OperationModuleEnum as module>
									<option value="${module.name()}" >${I18n["operation_module_" + module.name()]!module.name()}</option>
								</#list>
							</select>
						</div>
					</div>
					<div class="col-xs-2">
						<div class="input-group">
							<span class="input-group-addon">${I18n.operation_type}</span>
							<select class="form-control" id="operationType" >
								<option value="" >${I18n.operation_type_all}</option>
								<#list OperationTypeEnum as type>
									<option value="${type.name()}" >${I18n["operation_type_" + type.name()]!type.name()}</option>
								</#list>
							</select>
						</div>
					</div>
					<div class="col-xs-2">
						<div class="input-group">
							<span class="input-group-addon">${I18n.operator_name}</span>
							<input type="text" class="form-control" id="operatorName" placeholder="${I18n.system_please_input}" >
						</div>
					</div>
					<div class="col-xs-2">
						<div class="input-group">
							<span class="input-group-addon">${I18n.jobinfo_field_jobgroup}</span>
							<select class="form-control" id="jobGroup"  >
								<option value="" >${I18n.system_selected_nothing}</option>
								<#list JobGroupList as group>
									<option value="${group.id}" >${group.title}</option>
								</#list>
							</select>
						</div>
					</div>
					<div class="col-xs-3">
						<div class="input-group">
                			<span class="input-group-addon">
	                  			${I18n.operation_time}
	                		</span>
							<input type="text" class="form-control" id="filterTime" readonly >
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
<#--daterangepicker-->
<script src="${request.contextPath}/static/adminlte/bower_components/moment/moment.min.js"></script>
<script src="${request.contextPath}/static/adminlte/bower_components/bootstrap-daterangepicker/daterangepicker.js"></script>
<#-- admin table -->
<script src="${request.contextPath}/static/biz/common/admin.table.js"></script>
<script>
	$(function() {

		// ---------------------- filter ----------------------

		/**
		 * filter Time
		 */
		var rangesConf = {};
		rangesConf[I18n.daterangepicker_ranges_today] = [moment().startOf('day'), moment().endOf('day')];
		rangesConf[I18n.daterangepicker_ranges_yesterday] = [moment().subtract(1, 'days').startOf('day'), moment().subtract(1, 'days').endOf('day')];
		rangesConf[I18n.daterangepicker_ranges_this_month] = [moment().startOf('month'), moment().endOf('month')];
		rangesConf[I18n.daterangepicker_ranges_last_month] = [moment().subtract(1, 'months').startOf('month'), moment().subtract(1, 'months').endOf('month')];
		rangesConf[I18n.daterangepicker_ranges_recent_week] = [moment().subtract(1, 'weeks').startOf('day'), moment().endOf('day')];
		rangesConf[I18n.daterangepicker_ranges_recent_month] = [moment().subtract(1, 'months').startOf('day'), moment().endOf('day')];

		$('#filterTime').daterangepicker({
			autoApply:false,
			singleDatePicker:false,		// 范围选择 or 单时间选择
			showDropdowns:true,         // 年月 选择条件是否为下拉框
			timePicker: true,
			timePicker24Hour: true,
			timePickerSeconds: true,	// 时间选择是否显示秒
			opens : 'left', 			// 日期选择框的弹出位置
			ranges: rangesConf,
			locale : {
				format: 'YYYY-MM-DD HH:mm:ss',
				separator : ' - ',
				customRangeLabel : I18n.daterangepicker_custom_name ,
				applyLabel : I18n.system_ok ,
				cancelLabel : I18n.system_cancel ,
				fromLabel : I18n.daterangepicker_custom_starttime ,
				toLabel : I18n.daterangepicker_custom_endtime ,
				daysOfWeek : I18n.daterangepicker_custom_daysofweek.split(',') ,        // '日', '一', '二', '三', '四', '五', '六'
				monthNames : I18n.daterangepicker_custom_monthnames.split(',') ,        // '一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '十一月', '十二月'
				firstDay : 1
			}
		});

		// init filter
		function resetFilter(){
			$('#filterTime').data("daterangepicker").setStartDate( rangesConf[I18n.daterangepicker_ranges_recent_week][0] );
			$('#filterTime').data("daterangepicker").setEndDate( rangesConf[I18n.daterangepicker_ranges_recent_week][1] );

			$('#operationModule').val('');
			$('#operationType').val('');
			$('#operatorName').val('');
			$('#jobGroup').val('');
		}
		resetFilter();

		// ---------------------- page ----------------------

		/**
		 * init table
		 */
		$.adminTable.initTable({
			table: '#data_list',
			url: base_url + "/joboperationlog/pageList",
			queryParams: function (params) {
				var obj = {};
				obj.operationModule = $('#operationModule').val();
				obj.operationType = $('#operationType').val();
				obj.operatorName = $('#operatorName').val();
				obj.filterTime = $('#filterTime').val();
				obj.jobGroup = $('#jobGroup').val();
				obj.offset = params.offset;
				obj.pagesize = params.limit;
				return obj;
			},
			resetHandler : function() {
				// reset filter
				resetFilter();
			},
			columns:[
				{
					title: 'ID',
					field: 'id',
					width: '5',
					widthUnit: '%',
					align: 'left'
				},
				{
					title: '${I18n.operation_module}',
					field: 'operationModule',
					width: '10',
					widthUnit: '%',
					align: 'left',
					formatter: function(value, row, index) {
						return I18n["operation_module_" + value] || value;
					}
				},
				{
					title: '${I18n.operation_type}',
					field: 'operationType',
					width: '10',
					widthUnit: '%',
					align: 'left',
					formatter: function(value, row, index) {
						return I18n["operation_type_" + value] || value;
					}
				},
				{
					title: '${I18n.operator_name}',
					field: 'operatorName',
					width: '10',
					widthUnit: '%',
					align: 'left'
				},
				{
					title: '${I18n.operation_time}',
					field: 'operationTime',
					width: '15',
					widthUnit: '%',
					formatter: function(value, row, index) {
						return value?moment(value).format("YYYY-MM-DD HH:mm:ss"):"";
					}
				},
				{
					title: '${I18n.operation_detail}',
					field: 'operationDetail',
					width: '30',
					widthUnit: '%',
					align: 'left',
					formatter: function(value, row, index) {
						if (row.operationModule == 'LOGIN') {
							return '${I18n.login_ip}: ' + (row.operationIp || '');
						} else if (row.operationModule == 'USER_MANAGEMENT') {
							return '${I18n.target_user}: ' + (row.targetUser || '');
						} else if (row.operationModule == 'EXECUTOR_MANAGEMENT') {
							return '${I18n.executor_name}: ' + (row.targetName || '');
						} else if (row.operationModule == 'JOB_INFO') {
							return '${I18n.executor_name}: ' + (row.jobGroupName || '') + ', ${I18n.job_desc}: ' + (row.targetName || '');
						}
						return '';
					}
				},
				{
					title: '${I18n.operation_ip}',
					field: 'operationIp',
					width: '10',
					widthUnit: '%',
					align: 'left'
				}
			]
		});

	});
</script>
<!-- 3-script end -->

</body>
</html>