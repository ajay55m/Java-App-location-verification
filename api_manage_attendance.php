<?php
error_reporting(0);
ini_set('display_errors', 0);
header('Content-Type: application/json; charset=utf-8');

require_once "database/init.php";

$action = isset($_REQUEST['action']) ? trim($_REQUEST['action']) : 'get_feed';

if ($action === 'update_break') {
    // -------------------- Update Break Hours --------------------
    $attend_id = isset($_POST['id']) ? mysqli_real_escape_string($con, $_POST['id']) : (isset($_GET['id']) ? mysqli_real_escape_string($con, $_GET['id']) : '');
    $took_break = isset($_POST['took_break']) ? intval($_POST['took_break']) : (isset($_GET['took_break']) ? intval($_GET['took_break']) : 0);

    if (empty($attend_id)) {
        echo json_encode([
            "status" => "error",
            "message" => "Missing attendance ID"
        ]);
        exit;
    }

    $break_hours = ($took_break > 0) ? 1.0 : 0.0;
    $update_q = "UPDATE labours_attendance SET break_hours = '$break_hours' WHERE id = '$attend_id'";
    $res = mysqli_query($con, $update_q);

    if ($res) {
        echo json_encode([
            "status" => "success",
            "message" => "Break status updated successfully",
            "attend_id" => $attend_id,
            "break_hours" => $break_hours
        ]);
    } else {
        echo json_encode([
            "status" => "error",
            "message" => "Failed to update break status: " . mysqli_error($con)
        ]);
    }
    exit;
}

// -------------------- Default: Get Attendance & Move Feed --------------------
$uid = isset($_GET['uid']) ? mysqli_real_escape_string($con, $_GET['uid']) : '';
$projid = isset($_GET['projname']) ? mysqli_real_escape_string($con, $_GET['projname']) : (isset($_GET['projid']) ? mysqli_real_escape_string($con, $_GET['projid']) : 'all');

$current_day   = date('d');
$current_month = date('m');
$current_year  = date('Y');

$sel_day   = isset($_GET['f_day'])   ? mysqli_real_escape_string($con, $_GET['f_day'])   : $current_day;
$sel_month = isset($_GET['f_month']) ? mysqli_real_escape_string($con, $_GET['f_month']) : $current_month;
$sel_year  = isset($_GET['f_year'])  ? mysqli_real_escape_string($con, $_GET['f_year'])  : $current_year;

// Format day and month with leading zero if single digit
$sel_day   = str_pad($sel_day, 2, "0", STR_PAD_LEFT);
$sel_month = str_pad($sel_month, 2, "0", STR_PAD_LEFT);

$target_date = "$sel_year-$sel_month-$sel_day";

// User Name
$user_name = "User";
if (!empty($uid)) {
    $sql1 = mysqli_query($con, "SELECT username FROM app_users WHERE id = '$uid'");
    if ($sql1 && $getsql1 = mysqli_fetch_array($sql1)) {
        $user_name = $getsql1['username'];
    }
}

// Projects allocated to user
$allocated_projects = [];
$assigned_ids = [];

if (!empty($uid)) {
    $proj_q = mysqli_query($con, "
        SELECT p.id, p.projname
        FROM project p
        INNER JOIN assignsubcontractorproject a ON a.project_id = p.id
        WHERE a.user_id = '$uid'
        ORDER BY p.projname ASC
    ");

    if ($proj_q) {
        while ($row = mysqli_fetch_assoc($proj_q)) {
            $allocated_projects[] = [
                "id" => (string)$row['id'],
                "projname" => (string)$row['projname']
            ];
            $assigned_ids[] = (string)$row['id'];
        }
    }
}

// Construct WHERE clause
if ($projid === 'all' || empty($projid)) {
    if (!empty($assigned_ids)) {
        $proj_filter_list = "'" . implode("','", $assigned_ids) . "'";
        $project_where_clause = "a.projectid IN ($proj_filter_list)";
        $move_project_where_clause = "h.projectid IN ($proj_filter_list)";
    } else {
        $project_where_clause = "1=0";
        $move_project_where_clause = "1=0";
    }
} else {
    $project_where_clause = "a.projectid = '$projid'";
    $move_project_where_clause = "h.projectid = '$projid'";
}

// Attendance feed
$records = [];
if (!empty($assigned_ids)) {
    $sql = "SELECT e.first_name, a.id as attend_id, a.timein, a.timeout, a.break_hours, p.projname,
                   (a.image_name_in IS NOT NULL AND a.image_name_in != '') as has_in,
                   (a.image_name_out IS NOT NULL AND a.image_name_out != '') as has_out
            FROM labours_attendance a
            INNER JOIN employees e ON e.id = a.empid
            INNER JOIN project p ON p.id = a.projectid
            WHERE $project_where_clause
              AND a.attendance_date = '$target_date'
            ORDER BY a.timein DESC";
    $res = mysqli_query($con, $sql);
    if ($res) {
        while ($row = mysqli_fetch_assoc($res)) {
            $attend_id = (string)$row['attend_id'];
            $has_in  = (bool)$row['has_in'];
            $has_out = (bool)$row['has_out'];

            $in_url  = $has_in  ? "view_attendance_img.php?attendance_id=" . $attend_id . "&type=in"  : "";
            $out_url = $has_out ? "view_attendance_img.php?attendance_id=" . $attend_id . "&type=out" : "";

            $records[] = [
                "attend_id"    => $attend_id,
                "first_name"   => (string)$row['first_name'],
                "projname"     => (string)$row['projname'],
                "timein"       => $row['timein'] ? (string)$row['timein'] : "",
                "timeout"      => $row['timeout'] ? (string)$row['timeout'] : "",
                "break_hours"  => floatval($row['break_hours']),
                "has_in"       => $has_in,
                "has_out"      => $has_out,
                "in_photo_url" => $in_url,
                "out_photo_url"=> $out_url
            ];
        }
    }
}

// Next Site Move feed
$move_records = [];
if (!empty($assigned_ids)) {
    $sql_move = "SELECT e.first_name, h.id as move_id, h.in_time, h.out_time, p.projname,
                        (h.in_image IS NOT NULL AND h.in_image != '') as has_in,
                        (h.out_image IS NOT NULL AND h.out_image != '') as has_out
                 FROM hrms_attendance h
                 INNER JOIN employees e ON e.id = h.employee_id
                 INNER JOIN project p ON p.id = h.projectid
                 INNER JOIN labours_attendance a ON a.empid = h.employee_id
                        AND a.attendance_date = h.attendance_date
                        AND a.projectid = h.projectid
                 WHERE $move_project_where_clause
                   AND h.attendance_date = '$target_date'
                   AND h.flag = 'movement'
                   AND a.timeout IS NOT NULL AND a.timeout != ''
                 ORDER BY h.in_time DESC";
    $res_move = mysqli_query($con, $sql_move);
    if ($res_move) {
        while ($row = mysqli_fetch_assoc($res_move)) {
            $move_id = (string)$row['move_id'];
            $has_in  = (bool)$row['has_in'];
            $has_out = (bool)$row['has_out'];

            $in_url  = $has_in  ? "view_attendance_img.php?attendance_id=" . $move_id . "&source=hrms&type=in"  : "";
            $out_url = $has_out ? "view_attendance_img.php?attendance_id=" . $move_id . "&source=hrms&type=out" : "";

            $move_records[] = [
                "move_id"      => $move_id,
                "first_name"   => (string)$row['first_name'],
                "projname"     => (string)$row['projname'],
                "in_time"      => $row['in_time'] ? (string)$row['in_time'] : "",
                "out_time"     => $row['out_time'] ? (string)$row['out_time'] : "",
                "has_in"       => $has_in,
                "has_out"      => $has_out,
                "in_photo_url" => $in_url,
                "out_photo_url"=> $out_url
            ];
        }
    }
}

echo json_encode([
    "status"             => "success",
    "user_name"          => $user_name,
    "target_date"        => $target_date,
    "selected_day"       => $sel_day,
    "selected_month"     => $sel_month,
    "selected_year"      => $sel_year,
    "allocated_projects" => $allocated_projects,
    "records"            => $records,
    "move_records"       => $move_records
]);
exit;
