package com.anexohorror.client;

import com.anexohorror.AnexoHorror;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Cargador minimo de .obj triangulado (v / vt / vn / f a/b/c). Se carga una sola vez. */
public final class ObjModel {
    public final float[] pos;
    public final float[] uv;
    public final float[] nrm;
    public final int[] idx;
    public final float minY;
    public final float height;
    public final float cx;
    public final float cz;

    private static ObjModel slender;
    private static boolean failed;

    public static ObjModel slender() {
        if (slender == null && !failed) {
            try {
                slender = load(new ResourceLocation(AnexoHorror.MODID, "obj/slender.obj"));
            } catch (Exception e) {
                failed = true;
                LogUtils.getLogger().error("AnexoHorror: no se pudo cargar obj/slender.obj", e);
            }
        }
        return slender;
    }

    private ObjModel(float[] pos, float[] uv, float[] nrm, int[] idx) {
        this.pos = pos;
        this.uv = uv;
        this.nrm = nrm;
        this.idx = idx;
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, mnY = Float.MAX_VALUE, mxY = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (int i = 0; i < pos.length; i += 3) {
            minX = Math.min(minX, pos[i]); maxX = Math.max(maxX, pos[i]);
            mnY = Math.min(mnY, pos[i + 1]); mxY = Math.max(mxY, pos[i + 1]);
            minZ = Math.min(minZ, pos[i + 2]); maxZ = Math.max(maxZ, pos[i + 2]);
        }
        this.minY = mnY;
        this.height = mxY - mnY;
        this.cx = (minX + maxX) * 0.5F;
        this.cz = (minZ + maxZ) * 0.5F;
    }

    private static ObjModel load(ResourceLocation rl) throws Exception {
        List<float[]> v = new ArrayList<>();
        List<float[]> vt = new ArrayList<>();
        List<float[]> vn = new ArrayList<>();
        List<int[]> faces = new ArrayList<>();
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(rl);
             BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.length() < 3) continue;
                String[] t = line.trim().split("\\s+");
                switch (t[0]) {
                    case "v" -> v.add(new float[]{f(t[1]), f(t[2]), f(t[3])});
                    case "vt" -> vt.add(new float[]{f(t[1]), 1.0F - f(t[2])}); // OBJ: v hacia arriba; Minecraft: hacia abajo
                    case "vn" -> vn.add(new float[]{f(t[1]), f(t[2]), f(t[3])});
                    case "f" -> {
                        for (int i = 1; i <= 3; i++) {
                            String[] p = t[i].split("/");
                            faces.add(new int[]{Integer.parseInt(p[0]) - 1,
                                    Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]) - 1});
                        }
                    }
                    default -> { }
                }
            }
        }
        // Expandimos a un vertice por esquina de cara (v/vt/vn pueden diferir).
        int n = faces.size();
        float[] pos = new float[n * 3];
        float[] uv = new float[n * 2];
        float[] nrm = new float[n * 3];
        int[] idx = new int[n];
        for (int i = 0; i < n; i++) {
            int[] c = faces.get(i);
            float[] a = v.get(c[0]);
            float[] b = vt.get(c[1]);
            float[] d = vn.get(c[2]);
            pos[i * 3] = a[0]; pos[i * 3 + 1] = a[1]; pos[i * 3 + 2] = a[2];
            uv[i * 2] = b[0]; uv[i * 2 + 1] = b[1];
            nrm[i * 3] = d[0]; nrm[i * 3 + 1] = d[1]; nrm[i * 3 + 2] = d[2];
            idx[i] = i;
        }
        return new ObjModel(pos, uv, nrm, idx);
    }

    private static float f(String s) { return Float.parseFloat(s); }
}
