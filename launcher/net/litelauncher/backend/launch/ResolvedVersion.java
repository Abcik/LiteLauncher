package net.litelauncher.backend.launch;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;

import java.util.List;

record ResolvedVersion(
        String selectedId,
        String id,
        String jarId,
        String clientDownloadId,
        boolean jarExplicit,
        String mainClass,
        String assets,
        int javaMajor,
        JsonObject clientDownload,
        JsonObject assetIndex,
        List<JsonObject> libraries,
        JsonArray jvmArguments,
        JsonArray gameArguments,
        String minecraftArguments,
        String type,
        String clientType,
        long clientReleaseTime
) {
}
