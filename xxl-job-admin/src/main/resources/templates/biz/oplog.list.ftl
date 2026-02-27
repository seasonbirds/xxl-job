<!DOCTYPE html>
<html>
<head>
    <#-- import macro -->
    <#import "../common/common.macro.ftl" as netCommon>

    <!-- 1-style start -->
    <@netCommon.commonStyle />
    <link rel="stylesheet" href="${request.contextPath}/static/plugins/bootstrap-table/bootstrap-table.min.css">
    <!-- daterangepicker -->
    <link rel="stylesheet"
          href="${request.contextPath}/static/adminlte/bower_components/bootstrap-daterangepicker/daterangepicker.css">
    <!-- 1-style end -->

</head>
<body class="hold-transition" style="background-color: #ecf0f5;">
<div class="wrapper">
    <section class="content">

        <!-- 2-content start -->

        <#-- 查询区域 -->
        <div class="box" style="margin-bottom:9px;">
            <div class="box-body">
                <div class="row" id="data_filter">

                    <div class="col-xs-2">
                        <div class="input-group">
                            <span class="input-group-addon">${I18n.oplog_module}</span>
                            <select class="form-control" id="module">
                                <option value="">${I18n.system_all}</option>
                                <option value="LOGIN">${I18n.oplog_module_login}</option>
                                <option value="USER">${I18n.oplog_module_user}</option>
                                <option value="JOB_GROUP">${I18n.oplog_module_jobgroup}</option>
                                <option value="JOB_INFO">${I18n.oplog_module_jobinfo}</option>
                            </select>
                        </div>
                    </div>
                    <div class="col-xs-2">
                        <div class="input-group">
                            <span class="input-group-addon">${I18n.oplog_type}</span>
                            <select class="form-control" id="type">
                                <option value="">${I18n.system_all}</option>
                                <option value="LOGIN">${I18n.oplog_type_login}</option>
                                <option value="LOGOUT">${I18n.oplog_type_logout}</option>
                                <option value="ADD">${I18n.oplog_type_add}</option>
                                <option value="UPDATE">${I18n.oplog_type_update}</option>
                                <option value="DELETE">${I18n.oplog_type_delete}</option>
                                <option value="START">${I18n.oplog_type_start}</option>
                                <option value="STOP">${I18n.oplog_type_stop}</option>
                            </select>
                        </div>
                    </div>
                    <div class="col-xs-2">
                        <div class="input-group">
                            <span class="input-group-addon">${I18n.oplog_operator}</span>
                            <input type="text" class="form-control" id="operator" autocomplete="on">
                        </div>
                    </div>
                    <div class="col-xs-2">
                        <div class="input-group">
                            <span class="input-group-addon">${I18n.oplog_jobgroup}</span>
                            <select class="form-control" id="jobGroup">
                                <option value="0">${I18n.system_all}</option>
                            </select>
                        </div>
                    </div>
                    <div class="col-xs-2">
                        <div class="input-group">
                            <span class="input-group-addon">
                                ${I18n.oplog_op_time}
                            </span>
                            <input type="text" class="form-control" id="filterTime" readonly>
                        </div>
                    </div>

                    <div class="col-xs-1">
                        <button class="btn btn-block btn-primary searchBtn">${I18n.system_search}</button>
                    </div>
                    <div class="col-xs-1">
                        <button class="btn btn-block btn-default resetBtn">${I18n.system_reset}</button>
                    </div>
                </div>
            </div>
        </div>

        <#-- 数据表格区域 -->
        <div class="row">
            <div class="col-xs-12">
                <div class="box">
                    <div class="box-body">
                        <table id="data_list" class="table table-bordered table-striped" width="100%">
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
    $(function () {

        // module name map
        var moduleMap = {
            'LOGIN': '${I18n.oplog_module_login}',
            'USER': '${I18n.oplog_module_user}',
            'JOB_GROUP': '${I18n.oplog_module_jobgroup}',
            'JOB_INFO': '${I18n.oplog_module_jobinfo}'
        };

        // type name map
        var typeMap = {
            'LOGIN': '${I18n.oplog_type_login}',
            'LOGOUT': '${I18n.oplog_type_logout}',
            'ADD': '${I18n.oplog_type_add}',
            'UPDATE': '${I18n.oplog_type_update}',
            'DELETE': '${I18n.oplog_type_delete}',
            'START': '${I18n.oplog_type_start}',
            'STOP': '${I18n.oplog_type_stop}'
        };

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
            autoApply: false,
            singleDatePicker: false,
            showDropdowns: true,
            timePicker: true,
            timePicker24Hour: true,
            timePickerSeconds: true,
            opens: 'left',
            ranges: rangesConf,
            locale: {
                format: 'YYYY-MM-DD HH:mm:ss',
                separator: ' - ',
                customRangeLabel: I18n.daterangepicker_custom_name,
                applyLabel: I18n.system_ok,
                cancelLabel: I18n.system_cancel,
                fromLabel: I18n.daterangepicker_custom_starttime,
                toLabel: I18n.daterangepicker_custom_endtime,
                daysOfWeek: I18n.daterangepicker_custom_daysofweek.split(','),
                monthNames: I18n.daterangepicker_custom_monthnames.split(','),
                firstDay: 1
            }
        });

        // init filter
        function resetFilter() {
            $('#filterTime').data("daterangepicker").setStartDate(rangesConf[I18n.daterangepicker_ranges_recent_week][0]);
            $('#filterTime').data("daterangepicker").setEndDate(rangesConf[I18n.daterangepicker_ranges_recent_week][1]);

            $("#module").val('');
            $("#type").val('');
            $("#operator").val('');
            $("#jobGroup").val(0);
        }

        resetFilter();

        // load jobGroup list
        $.ajax({
            type: 'POST',
            url: base_url + '/jobgroup/all',
            dataType: 'json',
            success: function (data) {
                if (data.code == 200) {
                    var list = data.data;
                    for (var i = 0; i < list.length; i++) {
                        var item = list[i];
                        $("#jobGroup").append('<option value="' + item.id + '">' + item.title + '</option>');
                    }
                }
            }
        });

        // ---------------------- page ----------------------

        /**
         * init table
         */
        $.adminTable.initTable({
            table: '#data_list',
            url: base_url + "/oplog/pageList",
            queryParams: function (params) {
                var obj = {};
                obj.module = $('#module').val();
                obj.type = $('#type').val();
                obj.operator = $('#operator').val();
                obj.jobGroup = $('#jobGroup').val();
                obj.filterTime = $('#filterTime').val();
                obj.offset = params.offset;
                obj.pagesize = params.limit;
                return obj;
            },
            resetHandler: function () {
                resetFilter();
            },
            columns: [
                {
                    title: 'ID',
                    field: 'id',
                    width: '5',
                    widthUnit: '%',
                    align: 'center'
                },
                {
                    title: I18n.oplog_module,
                    field: 'module',
                    width: '10',
                    widthUnit: '%',
                    align: 'center',
                    formatter: function (value, row, index) {
                        return moduleMap[value] || value;
                    }
                },
                {
                    title: I18n.oplog_type,
                    field: 'type',
                    width: '8',
                    widthUnit: '%',
                    align: 'center',
                    formatter: function (value, row, index) {
                        return typeMap[value] || value;
                    }
                },
                {
                    title: I18n.oplog_operator,
                    field: 'operator',
                    width: '10',
                    widthUnit: '%',
                    align: 'center'
                },
                {
                    title: I18n.oplog_op_time,
                    field: 'opTime',
                    width: '15',
                    widthUnit: '%',
                    align: 'center',
                    formatter: function (value, row, index) {
                        return value ? moment(value).format("YYYY-MM-DD HH:mm:ss") : "";
                    }
                },
                {
                    title: I18n.oplog_op_ip,
                    field: 'opIp',
                    width: '10',
                    widthUnit: '%',
                    align: 'center'
                },
                {
                    title: I18n.oplog_target_name,
                    field: 'targetName',
                    width: '15',
                    widthUnit: '%',
                    align: 'center'
                },
                {
                    title: I18n.oplog_content,
                    field: 'content',
                    width: '27',
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
