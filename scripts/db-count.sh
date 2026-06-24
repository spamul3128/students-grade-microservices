#!/bin/bash
# Database record count script
# Shows the count of records in each table

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Database connection details
DB_HOST="localhost"
DB_PORT="5432"
DB_NAME="grades_db"
DB_USER="postgres"
DB_PASSWORD="postgres"

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# Execute SQL query and get result
exec_sql() {
    PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -A -c "$1"
}

# Get count for a table
get_count() {
    local table=$1
    local count=$(exec_sql "SELECT COUNT(*) FROM $table;")
    echo "$count"
}

# Check database connection
log_info "Connecting to database..."
if ! PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d postgres -c '\q' 2>/dev/null; then
    echo -e "${RED}[ERROR]${NC} Cannot connect to database"
    echo "Make sure PostgreSQL is running: docker-compose up -d postgres"
    exit 1
fi

log_success "Connected to database: $DB_NAME"
echo ""

# Display counts
echo "=========================================="
echo "  Database Record Counts"
echo "=========================================="
printf "%-25s %s\n" "Table" "Count"
echo "------------------------------------------"

tables=(
    "students"
    "teachers"
    "courses"
    "assignments"
    "grades"
    "course_grades"
    "users"
    "audit_logs"
)

total_records=0

for table in "${tables[@]}"; do
    count=$(get_count "$table")
    printf "%-25s %s\n" "$table" "$count"
    total_records=$((total_records + count))
done

echo "------------------------------------------"
printf "%-25s %s\n" "TOTAL" "$total_records"
echo "=========================================="
echo ""

# Show additional statistics
log_info "Additional Statistics:"

# Recent grades (last 5)
recent_grades=$(exec_sql "SELECT COUNT(*) FROM grades WHERE created_at > NOW() - INTERVAL '7 days';")
echo "  Grades added (last 7 days): $recent_grades"

# Active courses
active_courses=$(exec_sql "SELECT COUNT(DISTINCT course_id) FROM assignments;")
echo "  Courses with assignments: $active_courses"

# Students with grades
students_with_grades=$(exec_sql "SELECT COUNT(DISTINCT student_id) FROM grades;")
echo "  Students with grades: $students_with_grades"

# Average score
avg_score=$(exec_sql "SELECT ROUND(AVG(score)::numeric, 2) FROM grades;" | head -n 1)
if [ ! -z "$avg_score" ] && [ "$avg_score" != "" ]; then
    echo "  Average score: $avg_score"
fi

echo ""
log_success "Record count completed"

