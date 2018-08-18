rm -r -f release
javapackager -deploy -Bruntime=/Library/Java/JavaVirtualMachines/jdk-10.0.2.jdk/Contents/Home \
-native image \
-BjlinkOptions=compress=2 \
-outdir release -outfile Klikr.app \
--module-path /Users/volyx/Projects/klikr/klikr/target \
--module io.github.volyx.klikr/io.github.volyx.klikr.Main \
-name "Klikr" \
-title "Klikr" \
-Bverbose=true \
-Bicon=package/macosx/Klikr.icns \
-BappVersion=$1