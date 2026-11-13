#!/usr/bin/env python3
"""Regenera textures/entity/*_eyes.png (brillo emisivo de los ojos, estilo terror analogico).

Cada ojo = punto caliente casi blanco + halo suave + una fina estela horizontal (bloom de CRT),
con una ligera desconvergencia de color (rojo desplazado a un lado, azul al otro) y lineas de
barrido. Las posiciones estan medidas sobre los sprites (pixeles de textura); ajustalas aqui.
Los originales estan en tools/eyes_original/. Uso: python3 tools/gen_eyes.py
"""
import os
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'anexohorror', 'textures', 'entity')

# nombre: (tamano, color del halo (rgb 0..1), [(cx, cy, sigma_x, sigma_y, ganancia), ...])
ENT = {
    'peeker':  ((172, 448), (0.66, 0.90, 1.00), [(73.5, 50.0, 0.9, 1.1, 1.00), (91.5, 50.0, 0.9, 1.1, 0.88)]),
    'grin':    ((279, 448), (0.84, 0.95, 1.00), [(80.5, 18.5, 1.1, 1.1, 1.00), (97.0, 18.5, 1.1, 1.1, 0.92)]),
    'smiler':  ((286, 448), (1.00, 0.72, 0.36), [(117.5, 25.0, 0.7, 0.7, 0.90), (130.5, 24.5, 0.7, 0.7, 1.00)]),
    'drawn':   ((156, 448), (0.84, 1.00, 0.72), [(42.0, 53.5, 0.9, 1.0, 0.92), (65.5, 45.5, 0.9, 1.0, 1.00)]),
    'crawler': ((448, 441), (0.80, 1.00, 0.42), [(356.0, 57.0, 1.1, 1.1, 0.85), (369.5, 46.5, 1.5, 1.5, 1.00)]),
    'eye':     ((263, 448), (1.00, 0.22, 0.26), [(103.0, 47.0, 1.3, 1.3, 1.00), (135.0, 46.5, 1.3, 1.3, 0.90)]),
    'stalker': ((194, 448), (1.00, 0.24, 0.30), [(74.0, 27.0, 0.8, 0.9, 0.95), (87.5, 27.0, 0.8, 0.9, 1.00)]),
    'other':   ((1024, 1024), (1.00, 0.35, 0.40), [(676.0, 327.0, 1.5, 2.2, 0.95), (692.5, 327.0, 1.5, 2.2, 1.00)]),
}

HALO = 2.5      # sigma del halo respecto al del nucleo
HALO_AMP = 0.55
STREAK_AMP = 0.22
FRINGE = 0.6    # desplazamiento (px) rojo/azul


def eye_intensity(xx, yy, cx, cy, sx, sy, gain, shift):
    dx = xx - (cx + shift)
    dy = yy - cy
    core = np.exp(-(dx * dx / (2 * sx * sx) + dy * dy / (2 * sy * sy)))
    hx, hy = sx * HALO, sy * HALO
    halo = HALO_AMP * np.exp(-(dx * dx / (2 * hx * hx) + dy * dy / (2 * hy * hy)))
    streak = STREAK_AMP * np.exp(-(dx * dx / (2 * (sx * 6.0) ** 2) + dy * dy / (2 * 0.55 ** 2)))
    return np.clip(core + halo + streak, 0, 1) * gain


def build(name):
    (w, h), color, eyes = ENT[name]
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    chans = []
    for shift in (FRINGE, 0.0, -FRINGE):          # R, G, B
        i = np.zeros((h, w), np.float32)
        for cx, cy, sx, sy, g in eyes:
            i = np.maximum(i, eye_intensity(xx, yy, cx, cy, sx, sy, g, shift))
        chans.append(i)
    color = np.array(color, np.float32)
    # nucleo casi blanco, borde del color de la entidad
    out = np.zeros((h, w, 4), np.float32)
    alpha = np.maximum.reduce(chans)
    for c in range(3):
        white = np.clip((chans[c] - 0.55) / 0.4, 0, 1)
        hue = color[c] * (1 - white) + white
        out[:, :, c] = np.where(alpha > 1e-4, chans[c] * hue / np.maximum(alpha, 1e-4), 0)
    scan = 1.0 - 0.16 * (np.arange(h) % 2)[:, None]    # lineas de barrido
    out[:, :, 3] = alpha * scan
    out[:, :, :3] = np.clip(out[:, :, :3], 0, 1)
    return Image.fromarray((out * 255 + 0.5).astype(np.uint8), 'RGBA')


if __name__ == '__main__':
    for n in ENT:
        im = build(n)
        path = os.path.join(OUT, n + '_eyes.png')
        im.save(path, optimize=True)
        print(n, im.size, im.getchannel('A').getbbox())
