mat2 mat2_rotate_z(float radians) {
    return mat2(
        cos(radians), -sin(radians),
        sin(radians), cos(radians)
    );
}

int decodeUnsigned(int offsetX, int offsetY) {
    float texel = 1. / 128.;
    float yOffTexel = float(offsetY) * texel;

    int power = 1;
    int value = 0;
    for (int i = 0; i < 8; i++) {
        int x = offsetX + i;
        bool set = sign(length(texture(Sampler0, vec2(float(x) * texel, yOffTexel)).xyz)) > 0;
        if (set) value += power;
        power *= 2;
    }

    return value;
}

float decodeFixedPoint(int offsetX, int offsetY) {
    return float(decodeUnsigned(offsetX, offsetY)) / 255.0;
}

// Width / height of the screen. ScreenSize lives in the Globals uniform block since 1.21.6,
// which the text pipeline doesn't bind, so derive it from the perspective projection instead:
// its first two rows are the camera rows scaled by f / aspect and f (view bobbing only rotates them).
float screenAspectRatio() {
    vec3 row0 = vec3(ProjMat[0][0], ProjMat[1][0], ProjMat[2][0]);
    vec3 row1 = vec3(ProjMat[0][1], ProjMat[1][1], ProjMat[2][1]);
    return length(row1) / length(row0);
}
