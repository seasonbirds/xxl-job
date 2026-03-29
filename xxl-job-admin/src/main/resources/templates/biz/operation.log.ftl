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
							<span class="input-group-addon">操作模块</span>
							<select class="form-control" id="operationModule"  >
								<option value="">全部</option>
								<#list modules as module>
									<option value="${module}" >${module}</option>
								</#list>
							</select>
						</div>
					</div>
					<div class="col-xs-2">
						<div class="input-group">
							<span class="input-group-addon">操作类型</span>
							<select class="form-control" id="operationType" >
								<option value="">全部</option>
								<#list types as type>
									<option value="${type}" >${type}</option>
								</#list>
							</select>
						</div>
					</div>
					<div class="col-xs-2">
						<div class="input-group">
							<span class="input-group-addon">操作人</span>
							<input type="text" class="form-control" id="operator" >
						</div>
					</div>
					<div class="col-xs-2">
						<div class="input-group">
							<span class="input-group-addon">目标名称</span>
							<input type="text" class="form-control" id="targetName" >
						</div>
					</div>
					<div class="col-xs-3">
						<div class="input-group">
                		<span class="input-group-addon">
	                  		操作时间
	                	</span>
							<input type="text" class="form-control" id="filterTime" readonly >
						</div>
					</div>

					<div class="col-xs-1">
						<button class="btn btn-block btn-primary searchBtn" >查询</button>
					</div>
					<div class="col-xs-1">
						<button class="btn btn-block btn-default resetBtn" >重置</button>
					</div>
				</div>
			</div>
		</div>

		<#-- 数据表格区域 -->
		<div class="row">
			<div class="col-xs-12">
				<div class="box">
					<div class="box-header pull-left" id="data_operation" >
						<button class="btn btn-sm btn-danger clearLog" type="button"><i class="fa fa-remove "></i>清空日志</button>
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

		<!-- 日志清理.模态框 -->
		<div class="modal fade" id="clearLogModal" tabindex="-1" role="dialog"  aria-hidden="true">
			<div class="modal-dialog">
				<div class="modal-content">
					<div class="modal-header">
						<h4 class="modal-title" >清空操作日志</h4>
					</div>
					<div class="modal-body">
						<form class="form-horizontal form" role="form" >
							<div class="form-group">
								<label class="col-sm-3 control-label">操作时间范围：</label>
								<div class="col-sm-9">
									<div class="input-group">
										<input type="text" class="form-control" id="clearFilterTime" readonly >
									</div>
								</div>
							</div>

							<hr>
							<div class="form-group">
								<div class="col-sm-offset-3 col-sm-6">
									<button type="button" class="btn btn-primary ok" >确认</button>
									<button type="button" class="btn btn-default" data-dismiss="modal">取消</button>
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
		rangesConf["今天"] = [moment().startOf('day'), moment().endOf('day')];
		rangesConf["昨天"] = [moment().subtract(1, 'days').startOf('day'), moment().subtract(1, 'days').endOf('day')];
		rangesConf["本月"] = [moment().startOf('month'), moment().endOf('month')];
		rangesConf["上月"] = [moment().subtract(1, 'months').startOf('month'), moment().subtract(1, 'months').endOf('month')];
		rangesConf["最近一周"] = [moment().subtract(1, 'weeks').startOf('day'), moment().endOf('day')];
		rangesConf["最近一月"] = [moment().subtract(1, 'months').startOf('day'), moment().endOf('day')];

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
				customRangeLabel : "自定义" ,
				applyLabel : "确定" ,
				cancelLabel : "取消" ,
				fromLabel : "开始时间" ,
				toLabel : "结束时间" ,
				daysOfWeek : ['日', '一', '二', '三', '四', '五', '六'] ,
				monthNames : ['一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '十一月', '十二月'] ,
				firstDay : 1
			}
		});

		// clear filter time
		$('#clearFilterTime').daterangepicker({
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
				customRangeLabel : "自定义" ,
				applyLabel : "确定" ,
				cancelLabel : "取消" ,
				fromLabel : "开始时间" ,
				toLabel : "结束时间" ,
				daysOfWeek : ['日', '一', '二', '三', '四', '五', '六'] ,
				monthNames : ['一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '十一月', '十二月'] ,
				firstDay : 1
			}
		});

		// init filter
		function resetFilter(){
			$('#filterTime').data("daterangepicker").setStartDate( rangesConf["最近一周"][0] );
			$('#filterTime').data("daterangepicker").setEndDate( rangesConf["最近一周"][1] );

			$("#operationModule").val('');
			$("#operationType").val('');
			$("#operator").val('');
			$("#targetName").val('');
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
				obj.operationModule = $('#operationModule').val();
				obj.operationType = $('#operationType').val();
				obj.operator = $('#operator').val();
				obj.targetName = $('#targetName').val();
				obj.startTime = $('#filterTime').data('daterangepicker').startDate.format('YYYY-MM-DD HH:mm:ss');
				obj.endTime = $('#filterTime').data('daterangepicker').endDate.format('YYYY-MM-DD HH:mm:ss');
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
					title: '操作模块',
					field: 'operationModule',
					width: '10',
					widthUnit: '%',
					align: 'left'
				},
				{
					title: '操作类型',
					field: 'operationType',
					width: '10',
					widthUnit: '%',
					align: 'left'
				},
				{
					title: '操作人',
					field: 'operator',
					width: '10',
					widthUnit: '%',
					align: 'left'
				},
				{
					title: '操作时间',
					field: 'operationTime',
					width: '15',
					widthUnit: '%',
					formatter: function(value, row, index) {
						return value?moment(value).format("YYYY-MM-DD HH:mm:ss"):"";
					}
				},
				{
					title: '操作IP',
					field: 'operationIp',
					width: '10',
					widthUnit: '%',
					align: 'left'
				},
				{
					title: '目标ID',
					field: 'targetId',
					width: '8',
					widthUnit: '%',
					align: 'left'
				},
				{
					title: '目标名称',
					field: 'targetName',
					width: '15',
					widthUnit: '%',
					align: 'left',
					formatter: function(value, row, index) {
						if (value && value.length > 15) {
							return value.substr(0, 15) + '...';
						}
						return value;
					}
				},
				{
					title: '额外信息',
					field: 'targetExtra',
					width: '12',
					widthUnit: '%',
					align: 'left',
					formatter: function(value, row, index) {
						if (value && value.length > 10) {
							return value.substr(0, 10) + '...';
						}
						return value;
					}
				},
				{
					title: '备注',
					field: 'remark',
					width: '5',
					widthUnit: '%',
					align: 'left'
				}
			]
		});

		/**
		 * clear Log
		 */
		$('#data_operation').on('click', '.clearLog', function(){
			$('#clearLogModal').modal('show');
		});
		$("#clearLogModal .ok").on('click', function(){
			var startTime = $('#clearFilterTime').data('daterangepicker').startDate.format('YYYY-MM-DD HH:mm:ss');
			var endTime = $('#clearFilterTime').data('daterangepicker').endDate.format('YYYY-MM-DD HH:mm:ss');

			$.post(base_url + "/operationLog/deleteByTime", {
				"startTime": startTime,
				"endTime": endTime
			}, function(data, status) {
				if (data.code == 200) {
					$('#clearLogModal').modal('hide');
					layer.open({
						title: "提示",
						btn: ["确定"],
						content: "操作日志清空成功",
						icon: '1',
						end: function(layero, index){
							// refresh table
							$('#data_filter .searchBtn').click();
						}
					});
				} else {
					layer.open({
						title: "提示",
						btn: ["确定"],
						content: (data.msg || "操作日志清空失败"),
						icon: '2'
					});
				}
			});
		});
		$("#clearLogModal").on('hide.bs.modal', function () {
			$("#clearLogModal .form")[0].reset();
		});

	});
</script>
<!-- 3-script end -->

</body>
</html>
