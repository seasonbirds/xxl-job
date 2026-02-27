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
							<span class="input-group-addon">${I18n.operation_log_module}</span>
							<select class="form-control" id="module" >
								<option value="" >${I18n.system_all}</option>
								<#list moduleList as module>
									<option value="${module}" >${module.desc}</option>
								</#list>
							</select>
						</div>
					</div>
					<div class="col-xs-2">
						<div class="input-group">
							<span class="input-group-addon">${I18n.operation_log_type}</span>
							<select class="form-control" id="operationType" >
								<option value="" >${I18n.system_all}</option>
								<#list operationTypeList as operationType>
									<option value="${operationType}" >${operationType.desc}</option>
								</#list>
							</select>
						</div>
					</div>
					<div class="col-xs-2">
						<div class="input-group">
							<span class="input-group-addon">${I18n.operation_log_operator}</span>
							<input type="text" class="form-control" id="operator" placeholder="${I18n.operation_log_operator}" >
						</div>
					</div>
					<div class="col-xs-2">
						<div class="input-group">
							<span class="input-group-addon">${I18n.jobinfo_field_jobgroup}</span>
							<select class="form-control" id="jobGroup" >
								<option value="" >${I18n.system_all}</option>
								<#list JobGroupList as group>
									<option value="${group.id}" >${group.title}</option>
								</#list>
							</select>
						</div>
					</div>
					<div class="col-xs-3">
						<div class="input-group">
                		<span class="input-group-addon">
	                  		${I18n.joblog_field_triggerTime}
	                	</span>
							<input type="text" class="form-control" id="filterTime" readonly >
						</div>
					</div>

					<div class="col-xs-1">
						<button class="btn btn-block btn-primary searchBtn" >${I18n.system_search}</button>
					</div>
				</div>
			</div>
		</div>

		<#-- 数据表格区域 -->
		<div class="row">
			<div class="col-xs-12">
				<div class="box">
					<div class="box-header pull-left" id="data_operation" >
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

			$('#module').val('');
			$('#operationType').val('');
			$('#operator').val('');
			$('#jobGroup').val('');
		}
		resetFilter();

		// ---------------------- page ----------------------

		/**
		 * init table
		 */
		$.adminTable.initTable({
			table: '#data_list',
			url: base_url + "/operationLog/pageList",
			queryParams: function (params) {
				var obj = {};
				obj.module = $('#module').val();
				obj.operationType = $('#operationType').val();
				obj.operator = $('#operator').val();
				obj.jobGroup = $('#jobGroup').val();
				obj.filterTime = $('#filterTime').val();
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
					title: I18n.operation_log_module,
					field: 'module',
					width: '8',
					widthUnit: '%',
					align: 'center',
					formatter: function(value, row, index) {
						return value?value.desc:'';
					}
				},
				{
					title: I18n.operation_log_type,
					field: 'operationType',
					width: '8',
					widthUnit: '%',
					align: 'center',
					formatter: function(value, row, index) {
						return value?value.desc:'';
					}
				},
				{
					title: I18n.operation_log_operator,
					field: 'operator',
					width: '10',
					widthUnit: '%',
					align: 'center'
				},
				{
					title: I18n.operation_log_time,
					field: 'operationTime',
					width: '15',
					widthUnit: '%',
					formatter: function(value, row, index) {
						return value?moment(value).format("YYYY-MM-DD HH:mm:ss"):"";
					}
				},
				{
					title: I18n.operation_log_ip,
					field: 'ip',
					width: '10',
					widthUnit: '%',
					align: 'center'
				},
				{
					title: I18n.operation_log_target,
					field: 'targetName',
					width: '15',
					widthUnit: '%',
					formatter: function(value, row, index) {
						if (!value) return I18n.system_empty;
						var jobGroup = row.jobGroup;
						var module = row.module ? row.module.name : '';
						if (module == 'JOB' && jobGroup) {
							var groupName = $("#jobGroup").find("option[value='"+ jobGroup +"']").text();
							return '[' + groupName + '] ' + value;
						}
						if (value.length > 20) {
							return value.substr(0, 20) + '...';
						}
						return value;
					}
				},
				{
					title: I18n.operation_log_extra,
					field: 'extraInfo',
					width: '20',
					widthUnit: '%',
					formatter: function(value, row, index) {
						if (!value) return I18n.system_empty;
						if (value.length > 30) {
							return '<a class="logTips" href="javascript:;" >'+ I18n.system_show +'<span style="display:none;">'+ value +'</span></a>';
						}
						return value;
					}
				}
			]
		});

		// ---------------------- ComAlertTec ----------------------

		/**
		 * logTips alert
		 */
		$('body').on('click', '.logTips', function(){
			var msg = $(this).find('span').html();
			ComAlertTec.show(msg);
		});

		// Com Alert by Tec theme
		var ComAlertTec = {
			html:function(){
				var html =
						'<div class="modal fade" id="ComAlertTec" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true">' +
						'	<div class="modal-dialog modal-lg-">' +
						'		<div class="modal-content-tec">' +
						'			<div class="modal-body">' +
						'				<div class="alert" style="color:#fff;word-wrap: break-word;">' +
						'				</div>' +
						'			</div>' +
						'				<div class="modal-footer">' +
						'				<div class="text-center" >' +
						'					<button type="button" class="btn btn-info ok" data-dismiss="modal" >'+ I18n.system_ok +'</button>' +
						'				</div>' +
						'			</div>' +
						'		</div>' +
						'	</div>' +
						'</div>';
				return html;
			},
			show:function(msg, callback){
				// dom init
				if ($('#ComAlertTec').length == 0){
					$('body').append(ComAlertTec.html());
				}

				// init com alert
				$('#ComAlertTec .alert').html(msg);
				$('#ComAlertTec').modal('show');

				$('#ComAlertTec .ok').click(function(){
					$('#ComAlertTec').modal('hide');
					if(typeof callback == 'function') {
						callback();
					}
				});
			}
		};

	});
</script>
<!-- 3-script end -->

</body>
</html>