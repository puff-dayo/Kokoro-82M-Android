import android.content.Context
import org.jetbrains.bio.npy.NpyArray
import org.jetbrains.bio.npy.NpyFile
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class StyleLoader(private val context: Context) {
    
    private val names = listOf(
        "af",
        "af_bella",
        "af_nicole",
        "af_sarah",
        "af_sky",
        "am_adam",
        "am_michael",
        "bf_emma",
        "bf_isabella",
        "bm_george",
        "bm_lewis"
    )

    
    private val styleResourceMap: Map<String, Int> = names.associateWith { name ->
        val resourceName = name 
        val resourceId = context.resources.getIdentifier(resourceName, "raw", context.packageName)
        if (resourceId == 0) {
            throw IllegalArgumentException("Resource '$resourceName' not found in /res/raw")
        }
        resourceId
    }

    /**
     * 从 /res/raw/voices 加载指定 style 的 .npy 文件，并提取特定索引的 float32[1, 256] 向量。
     *
     * @param name style 名称（例如 "af_sarah"）
     * @param index 要提取的向量索引（范围：0 到 510）
     * @return float32[1, 256] 格式的 Tensor
     */
    fun getStyleArray(name: String, index: Int = 0): Array<FloatArray> {
        
        val resourceId = styleResourceMap[name]
            ?: throw IllegalArgumentException("Style '$name' not found in /res/raw")

        
        val inputStream: InputStream = context.resources.openRawResource(resourceId)

        
        val tempFile = File.createTempFile("temp_style", ".npy", context.cacheDir)
        tempFile.deleteOnExit() 
        FileOutputStream(tempFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }

        
        val npyArray: NpyArray = NpyFile.read(tempFile.toPath())

        
        if (npyArray.shape.size != 3 || npyArray.shape[0] != 511 || npyArray.shape[1] != 1 || npyArray.shape[2] != 256) {
            throw IllegalArgumentException("The loaded .npy file must have the shape (511, 1, 256)")
        }

        
        if (index < 0 || index >= 511) {
            throw IllegalArgumentException("Index must be between 0 and 510")
        }

        
        val styleArray = Array(1) { FloatArray(256) }
        val floatArray = npyArray.asFloatArray() 

        for (i in 0 until 256) {
            styleArray[0][i] = floatArray[index * 256 + i] 
        }

        return styleArray
    }
}