#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
task_tmp=$(mktemp -d)
trap 'rm -rf "$task_tmp"' EXIT
cat > "$task_tmp/CoreCheck.java" <<'JAVA'
import in.akhilesh.ember.Metrics;
import java.util.Arrays;
public class CoreCheck {
  static void check(boolean c){if(!c)throw new AssertionError();}
  public static void main(String[] args){
    var empty=new Metrics.NetworkResult(Arrays.asList(null,null));check(empty.median==null&&empty.variation==null&&empty.failures==2);
    var mixed=new Metrics.NetworkResult(Arrays.asList(90.0,10.0,null,30.0,50.0));check(mixed.median==40&&mixed.variation==50&&mixed.failures==1);
    check(Metrics.warnings(10,42f,true,false,true).size()==5);
    check(Metrics.warnings(-1,null,false,true,false).isEmpty());
    check(Metrics.duration(200,100)==0);
    check(Metrics.csv("a,\"b\"").equals("\"a,\"\"b\"\"\""));
    System.out.println("Core checks passed: failures, median, variation, warnings, missing data, duration and CSV.");
  }
}
JAVA
javac -d "$task_tmp" app/src/main/java/in/akhilesh/ember/Metrics.java "$task_tmp/CoreCheck.java"
java -cp "$task_tmp" CoreCheck
