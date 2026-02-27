<!DOCTYPE html>
<html>
<head>
	<#import "../common/common.macro.ftl" as netCommon>

	<@netCommon.commonStyle />
	<link rel="stylesheet" href="${request.contextPath}/static/plugins/bootstrap-table/bootstrap-table.min.css">
	<link rel="stylesheet" href="${request.contextPath}/static/adminlte/bower_components/bootstrap-daterangepicker/daterangepicker.css">

</head>
<body class="hold-transition" style="background-color: #ecf0f5;">
<div class="wrapper">
	<section class="content">

		<#-- 查询区域 -->
		<div class="box" style="margin-bottom:9px;">
			<div class="box-body">
				<div class="row" id="data_filter" >

					<div class="col-xs-2">
						<div class="input-group">
							<span class="input-group-addon">${I18n.operationlog_module}</span>
							<select class="form-control" id="module" >
								<option value="" >${I18n.system_all}</option>
								<option value="LOGIN" >${I18n.operationlog_module_login}</option>
								<option value="USER" >${I18n.operationlog_module_user}</option>
								<option value="JOBGROUP" >${I18n.operationlog_module_jobgroup}</option>
								<option value="JOBINFO" >${I18n.operationlog_module_jobinfo}</option>
							</select>
						</div>
					</div>
					<div class="col-xs-2" id="div_operationType">
						<div class="input-group">
							<span class="input-group-addon">${I18n.operationlog_operation_type}</span>
							<select class="form-control" id="operationType" >
								<option value="" >${I18n.system_all}</option>
							</select>
						</div>
					</div>
					<div class="col-xs-2" id="div_operator">
						<div class="input-group">
							<span class="input-group-addon">${I18n.operationlog_operator}</span>
							<input type="text" class="form-control" id="operator" autocomplete="on" >
						</div>
					</div>
					<div class="col-xs-2" id="div_filterTime">
						<div class="input-group">
							<span class="input-group-addon">${I18n.operationlog_operate_time}</span>
							<input type="text" class="form-control" id="filterTime" readonly >
						</div>
					</div>
					<div class="col-xs-2" id="div_targetName">
						<div class="input-group">
							<span class="input-group-addon" id="label_targetName">${I18n.operationlog_target_name}</span>
							<input type="text" class="form-control" id="targetName" autocomplete="on" >
						</div>
					</div>
					<div class="col-xs-2" id="div_jobGroupId" style="display:none;">
						<div class="input-group">
							<span class="input-group-addon">${I18n.operationlog_job_group}</span>
							<select class="form-control" id="jobGroupId" >
								<option value="-1" >${I18n.system_all}</option>
								<#if groupList?exists && groupList?size gt 0>
									<#list groupList as item>
										<option value="${item.id}" >${item.title}</option>
									</#list>
								</#if>
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
						<button class="btn btn-sm btn-warning cleanLog" type="button"><i class="fa fa-trash"></i>${I18n.operationlog_clean}</button>
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

		<#-- 日志清理模态框 -->
		<div class="modal fade" id="cleanModal" tabindex="-1" role="dialog"  aria-hidden="true">
			<div class="modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						<h4 class="modal-title" >${I18n.operationlog_clean_log}</h4>
					</div>
					<div class="modal-body">
						<form class="form-horizontal form" role="form" >
							<div class="form-group">
								<label class="col-sm-3 control-label">${I18n.joblog_clean_type}:</label>
								<div class="col-sm-9">
									<select class="form-control" name="type" >
										<option value="1" >${I18n.joblog_clean_type_1}</option>
										<option value="2" >${I18n.joblog_clean_type_2}</option>
										<option value="3" >${I18n.joblog_clean_type_3}</option>
										<option value="4" >${I18n.joblog_clean_type_4}</option>
										<option value="9" >${I18n.joblog_clean_type_9}</option>
									</select>
								</div>
							</div>
							<hr>
							<div class="form-group">
								<div class="col-sm-offset-3 col-sm-6">
									<button type="button" class="btn btn-primary ok" >${I18n.system_ok}</button>
									<button type="button" class="btn btn-default" data-dismiss="modal">${I18n.system_cancel}</button>
								</div>
							</div>
						</form>
					</div>
				</div>
			</div>
		</div>

	</section>
</div>

<@netCommon.commonScript />
<script src="${request.contextPath}/static/plugins/bootstrap-table/bootstrap-table.min.js"></script>
<script src="${request.contextPath}/static/plugins/bootstrap-table/locale/<#if I18n.admin_i18n?? && I18n.admin_i18n == 'en'>bootstrap-table-en-US.min.js<#else>bootstrap-table-zh-CN.min.js</#if>"></script>
<script src="${request.contextPath}/static/adminlte/bower_components/moment/moment.min.js"></script>
<script src="${request.contextPath}/static/adminlte/bower_components/bootstrap-daterangepicker/daterangepicker.js"></script>
<script src="${request.contextPath}/static/biz/common/admin.table.js"></script>
<script>
	$(function() {

		var rangesConf = {};
		rangesConf[I18n.daterangepicker_ranges_today] = [moment().startOf('day'), moment().endOf('day')];
		rangesConf[I18n.daterangepicker_ranges_yesterday] = [moment().subtract(1, 'days').startOf('day'), moment().subtract(1, 'days').endOf('day')];
		rangesConf[I18n.daterangepicker_ranges_this_month] = [moment().startOf('month'), moment().endOf('month')];
		rangesConf[I18n.daterangepicker_ranges_last_month] = [moment().subtract(1, 'months').startOf('month'), moment().subtract(1, 'months').endOf('month')];
		rangesConf[I18n.daterangepicker_ranges_recent_week] = [moment().subtract(6, 'days').startOf('day'), moment().endOf('day')];
		rangesConf[I18n.daterangepicker_ranges_recent_month] = [moment().subtract(29, 'days').startOf('day'), moment().endOf('day')];

		$("#filterTime").daterangepicker({
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

		$('#filterTime').data("daterangepicker").setStartDate(rangesConf[I18n.daterangepicker_ranges_recent_week][0]);
		$('#filterTime').data("daterangepicker").setEndDate(rangesConf[I18n.daterangepicker_ranges_recent_week][1]);

		function updateFilterVisibility() {
			var module = $('#module').val();
			var $operationType = $("#operationType");
			$operationType.empty();
			$operationType.append('<option value="" >' + I18n.system_all + '</option>');

			$('#div_operationType').show();
			$('#div_operator').show();
			$('#div_filterTime').show();
			$('#div_targetName').show();
			$('#div_jobGroupId').hide();

			if (module === 'LOGIN') {
				$('#div_operationType').hide();
				$('#div_targetName').hide();
				$('#label_targetName').text('${I18n.operationlog_target_name}');
			} else if (module === 'USER') {
				$operationType.append('<option value="新增" >' + I18n.system_opt_add + '</option>');
				$operationType.append('<option value="编辑" >' + I18n.system_opt_edit + '</option>');
				$operationType.append('<option value="删除" >' + I18n.system_opt_del + '</option>');
				$('#div_jobGroupId').hide();
				$('#label_targetName').text('${I18n.operationlog_target_name}');
			} else if (module === 'JOBGROUP') {
				$operationType.append('<option value="新增" >' + I18n.system_opt_add + '</option>');
				$operationType.append('<option value="编辑" >' + I18n.system_opt_edit + '</option>');
				$operationType.append('<option value="删除" >' + I18n.system_opt_del + '</option>');
				$('#div_jobGroupId').hide();
				$('#label_targetName').text('${I18n.operationlog_target_name}');
			} else if (module === 'JOBINFO') {
				$operationType.append('<option value="新增" >' + I18n.system_opt_add + '</option>');
				$operationType.append('<option value="编辑" >' + I18n.system_opt_edit + '</option>');
				$operationType.append('<option value="删除" >' + I18n.system_opt_del + '</option>');
				$operationType.append('<option value="启动" >' + I18n.jobinfo_opt_start + '</option>');
				$operationType.append('<option value="停止" >' + I18n.jobinfo_opt_stop + '</option>');
				$('#div_jobGroupId').show();
				$('#div_targetName').hide();
				$('#label_targetName').text('${I18n.operationlog_target_name}');
			} else {
				$operationType.append('<option value="登录" >' + I18n.operationlog_module_login + '</option>');
				$operationType.append('<option value="新增" >' + I18n.system_opt_add + '</option>');
				$operationType.append('<option value="编辑" >' + I18n.system_opt_edit + '</option>');
				$operationType.append('<option value="删除" >' + I18n.system_opt_del + '</option>');
				$operationType.append('<option value="启动" >' + I18n.jobinfo_opt_start + '</option>');
				$operationType.append('<option value="停止" >' + I18n.jobinfo_opt_stop + '</option>');
				$('#div_jobGroupId').show();
				$('#label_targetName').text('${I18n.operationlog_target_name}');
			}
		}

		$("#module").change(function() {
			updateFilterVisibility();
		});

		updateFilterVisibility();

		function initTable() {
			var postData = {};
			postData.module = $('#module').val();
			postData.operationType = $('#operationType').val();
			postData.operator = $('#operator').val();
			postData.targetName = $('#targetName').val();
			postData.jobGroupId = $('#jobGroupId').val();
			var filterTime = $('#filterTime').val();
			if (filterTime && filterTime.indexOf(' - ') > -1) {
				var timeArr = filterTime.split(' - ');
				postData.operateTimeStart = timeArr[0];
				postData.operateTimeEnd = timeArr[1];
			}
			return postData;
		}

		$.adminTable.initTable({
			table: '#data_list',
			url: base_url + "/operationlog/pageList",
			queryParams: function (params) {
				var obj = initTable();
				obj.offset = params.offset;
				obj.pagesize = params.limit;
				return obj;
			},
			resetHandler: function() {
				$('#filterTime').data("daterangepicker").setStartDate(rangesConf[I18n.daterangepicker_ranges_recent_week][0]);
				$('#filterTime').data("daterangepicker").setEndDate(rangesConf[I18n.daterangepicker_ranges_recent_week][1]);
				$('#module').prop('selectedIndex', 0);
				$('#operator').val('');
				$('#targetName').val('');
				$('#jobGroupId').val('-1');
				updateFilterVisibility();
			},
			columns:[
				{
					title: I18n.operationlog_module,
					field: 'module',
					width: '10',
					widthUnit: '%',
					formatter: function(value, row, index) {
						var moduleMap = {
							'LOGIN': I18n.operationlog_module_login,
							'USER': I18n.operationlog_module_user,
							'JOBGROUP': I18n.operationlog_module_jobgroup,
							'JOBINFO': I18n.operationlog_module_jobinfo
						};
						return moduleMap[value] || value;
					}
				},{
					title: I18n.operationlog_operation_type,
					field: 'operationType',
					width: '8',
					widthUnit: '%'
				},{
					title: I18n.operationlog_operator,
					field: 'operator',
					width: '10',
					widthUnit: '%'
				},{
					title: I18n.operationlog_operate_time,
					field: 'operateTime',
					width: '15',
					widthUnit: '%',
					formatter: function(value, row, index) {
						return value ? moment(value).format("YYYY-MM-DD HH:mm:ss") : "";
					}
				},{
					title: I18n.operationlog_ip,
					field: 'ip',
					width: '12',
					widthUnit: '%'
				},{
					title: I18n.operationlog_target_name,
					field: 'targetName',
					width: '15',
					widthUnit: '%'
				},{
					title: I18n.operationlog_job_group,
					field: 'jobGroupName',
					width: '10',
					widthUnit: '%'
				}
			]
		});

		$(".cleanLog").click(function () {
			$('#cleanModal').modal({backdrop: false, keyboard: false}).modal('show');
		});

		$("#cleanModal .ok").click(function () {
			var type = $('#cleanModal select[name="type"]').val();
			$.ajax({
				type: 'POST',
				url: base_url + "/operationlog/clearLog",
				data: {
					"type": type
				},
				dataType: "json",
				success: function(data){
					if (data.code == 200) {
						$('#cleanModal').modal('hide');
						layer.open({
							title: I18n.system_tips,
							btn: [I18n.system_ok],
							content: (I18n.system_success + ':' + data.msg),
							end: function(layero, index){
								$.adminTable.refresh();
							}
						});
					} else {
						layer.open({
							title: I18n.system_tips,
							btn: [I18n.system_ok],
							content: (data.msg || I18n.system_fail)
						});
					}
				}
			});
		});

	});

</script>

</body>
</html>
