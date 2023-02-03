/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.ide.common.vectordrawable

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.logging.Level
import java.util.logging.Logger
import java.util.regex.Pattern

/**
 * Converts SVG to VectorDrawable's XML.
 *
 *
 * There are two major functions:
 *
 *  * [.parse] Parses the .svg file, builds and optimizes an internal tree
 *  * [.writeFile] Traverses the internal tree and produces XML output
 *
 */
object Svg2Vector {
    private val logger = Logger.getLogger(Svg2Vector::class.java.simpleName)
    private const val SVG_DEFS = "defs"
    private const val SVG_USE = "use"
    const val SVG_HREF = "href"
    const val SVG_XLINK_HREF = "xlink:href"
    const val SVG_POLYGON = "polygon"
    const val SVG_POLYLINE = "polyline"
    const val SVG_RECT = "rect"
    const val SVG_CIRCLE = "circle"
    const val SVG_LINE = "line"
    const val SVG_PATH = "path"
    const val SVG_ELLIPSE = "ellipse"
    const val SVG_GROUP = "g"
    const val SVG_STYLE = "style"
    const val SVG_DISPLAY = "display"
    const val SVG_CLIP_PATH_ELEMENT = "clipPath"
    const val SVG_D = "d"
    const val SVG_CLIP = "clip"
    const val SVG_CLIP_PATH = "clip-path"
    const val SVG_CLIP_RULE = "clip-rule"
    const val SVG_FILL = "fill"
    const val SVG_FILL_OPACITY = "fill-opacity"
    const val SVG_FILL_RULE = "fill-rule"
    const val SVG_OPACITY = "opacity"
    const val SVG_PAINT_ORDER = "paint-order"
    const val SVG_STROKE = "stroke"
    const val SVG_STROKE_LINECAP = "stroke-linecap"
    const val SVG_STROKE_LINEJOIN = "stroke-linejoin"
    const val SVG_STROKE_OPACITY = "stroke-opacity"
    const val SVG_STROKE_WIDTH = "stroke-width"
    const val SVG_MASK = "mask"
    const val SVG_POINTS = "points"
    @JvmField
    val presentationMap = java.util.Map.ofEntries(
        java.util.Map.entry(SVG_CLIP, "android:clip"),
        java.util.Map.entry(SVG_CLIP_RULE, ""),  // Treated individually.
        java.util.Map.entry(SVG_FILL, "android:fillColor"),
        java.util.Map.entry(SVG_FILL_OPACITY, "android:fillAlpha"),
        java.util.Map.entry(SVG_FILL_RULE, "android:fillType"),
        java.util.Map.entry(SVG_OPACITY, ""),  // Treated individually.
        java.util.Map.entry(SVG_PAINT_ORDER, ""),  // Treated individually.
        java.util.Map.entry(SVG_STROKE, "android:strokeColor"),
        java.util.Map.entry(SVG_STROKE_LINECAP, "android:strokeLineCap"),
        java.util.Map.entry(SVG_STROKE_LINEJOIN, "android:strokeLineJoin"),
        java.util.Map.entry(SVG_STROKE_OPACITY, "android:strokeAlpha"),
        java.util.Map.entry(SVG_STROKE_WIDTH, "android:strokeWidth")
    )
    @JvmField
    val gradientMap = java.util.Map.ofEntries(
        java.util.Map.entry("x1", "android:startX"),
        java.util.Map.entry("y1", "android:startY"),
        java.util.Map.entry("x2", "android:endX"),
        java.util.Map.entry("y2", "android:endY"),
        java.util.Map.entry("cx", "android:centerX"),
        java.util.Map.entry("cy", "android:centerY"),
        java.util.Map.entry("r", "android:gradientRadius"),
        java.util.Map.entry("spreadMethod", "android:tileMode"),
        java.util.Map.entry("gradientUnits", ""),
        java.util.Map.entry("gradientTransform", ""),
        java.util.Map.entry("gradientType", "android:type")
    )

    // Set of all SVG nodes that we don't support. Categorized by the types.
    private val unsupportedSvgNodes = setOf( // Animation elements.
        "animate",
        "animateColor",
        "animateMotion",
        "animateTransform",
        "mpath",
        "set",  // Container elements.
        "a",
        "marker",
        "missing-glyph",
        "pattern",
        "switch",  // Filter primitive elements.
        "feBlend",
        "feColorMatrix",
        "feComponentTransfer",
        "feComposite",
        "feConvolveMatrix",
        "feDiffuseLighting",
        "feDisplacementMap",
        "feFlood",
        "feFuncA",
        "feFuncB",
        "feFuncG",
        "feFuncR",
        "feGaussianBlur",
        "feImage",
        "feMerge",
        "feMergeNode",
        "feMorphology",
        "feOffset",
        "feSpecularLighting",
        "feTile",
        "feTurbulence",  // Font elements.
        "font",
        "font-face",
        "font-face-format",
        "font-face-name",
        "font-face-src",
        "font-face-uri",
        "hkern",
        "vkern",  // Gradient elements.
        "stop",  // Graphics elements.
        "ellipse",
        "image",  // Light source elements.
        "feDistantLight",
        "fePointLight",
        "feSpotLight",  // Structural elements.
        "symbol",  // Text content elements.
        "altGlyphDef",
        "altGlyphItem",
        "glyph",
        "glyphRef",
        "text",  // Text content child elements.
        "altGlyph",
        "textPath",
        "tref",
        "tspan",  // Uncategorized elements.
        "color-profile",
        "cursor",
        "filter",
        "foreignObject",
        "script",
        "view"
    )
    private val SPACE_OR_COMMA = Pattern.compile("[\\s,]+")
    @Throws(IOException::class)
    private fun parse(fileName: String, inputStream: InputStream): SvgTree {
        val svgTree = SvgTree()
        val parseErrors: List<String> = ArrayList()
        val doc = svgTree.parse(fileName, inputStream, parseErrors)
        for (error in parseErrors) {
            svgTree.logError(error, null)
        }

        // Get <svg> elements.
        val svgNodes = doc.getElementsByTagName("svg")
        check(svgNodes.length == 1) { "Not a proper SVG file" }
        val rootElement = svgNodes.item(0) as Element
        svgTree.parseDimension(rootElement)
        if (svgTree.viewBox == null) {
            svgTree.logError("Missing \"viewBox\" in <svg> element", rootElement)
            return svgTree
        }
        val root = SvgGroupNode(svgTree, rootElement, "root")
        svgTree.setRoot(root)

        // Parse all the group and path nodes recursively.
        traverseSvgAndExtract(svgTree, root, rootElement)
        resolveUseNodes(svgTree)
        resolveGradientReferences(svgTree)

        // TODO: Handle clipPath elements that reference another clipPath
        // Add attributes for all the style elements.
        for ((key, value) in svgTree.styleAffectedNodes) {
            for (n in value) {
                addStyleToPath(n, svgTree.getStyleClassAttr(key))
            }
        }

        // Replaces elements that reference clipPaths and replaces them with clipPathNodes
        // Note that clip path can be embedded within style, so it has to be called after
        // addStyleToPath.
        for ((key, value) in svgTree.clipPathAffectedNodesSet) {
            handleClipPath(
                svgTree,
                key,
                value.first,
                value.second
            )
        }
        svgTree.flatten()
        svgTree.validate()
        svgTree.dump()
        return svgTree
    }

    // Fills in all <use> nodes in the svgTree.
    private fun resolveUseNodes(svgTree: SvgTree) {
        val nodes = svgTree.pendingUseSet
        while (!nodes.isEmpty()) {
            if (!nodes.removeIf { node: SvgGroupNode -> node.resolveHref(svgTree) }) {
                // Not able to make progress because of cyclic references.
                reportCycles(svgTree, nodes)
                break
            }
        }
    }

    // Resolves all href references in gradient nodes.
    private fun resolveGradientReferences(svgTree: SvgTree) {
        val nodes = svgTree.pendingGradientRefSet
        while (!nodes.isEmpty()) {
            if (!nodes.removeIf { node: SvgGradientNode -> node.resolveHref(svgTree) }) {
                // Not able to make progress because of cyclic references.
                reportCycles(svgTree, nodes)
                break
            }
        }
    }

    private fun <T : SvgNode?> reportCycles(
        svgTree: SvgTree, svgNodes: Set<T>
    ) {
        val edges: MutableMap<String?, String> = HashMap()
        val nodesById: MutableMap<String?, Node> = HashMap()
        for (svgNode in svgNodes) {
            val element = svgNode!!.documentElement
            val id = element.getAttribute("id")
            if (!id.isEmpty()) {
                val targetId = svgNode!!.hrefId
                if (!targetId.isEmpty()) {
                    edges[id] = targetId
                    nodesById[id] = element
                }
            }
        }
        while (!edges.isEmpty()) {
            val visited: MutableSet<String?> = HashSet()
            val entry: MutableMap.MutableEntry<String?, String> = edges.entries.iterator().next()
            var id = entry.key
            var targetId: String? = entry.value
            while (targetId != null && visited.add(id)) {
                id = targetId
                targetId = edges[id]
            }
            if (targetId != null) { // Broken links are reported separately. Ignore them here.
                val node = nodesById[id]
                val cycle = getCycleStartingAt(id!!, edges, nodesById)
                svgTree.logError("Circular dependency of <use> nodes: $cycle", node)
            }
            edges.keys.removeAll(visited)
        }
    }

    private fun getCycleStartingAt(
        startId: String,
        edges: Map<String?, String>,
        nodesById: Map<String?, Node>
    ): String {
        val buf = StringBuilder(startId)
        var id: String? = startId
        while (true) {
            id = edges[id]
            buf.append(" -> ").append(id)
            if (id == startId) {
                break
            }
            buf.append(" (line ").append(SvgTree.getStartLine(nodesById[id]!!)).append(")")
        }
        return buf.toString()
    }

    /** Traverse the tree in pre-order.  */
    private fun traverseSvgAndExtract(
        svgTree: SvgTree, currentGroup: SvgGroupNode, item: Element
    ) {
        val childNodes = item.childNodes
        for (i in 0 until childNodes.length) {
            val childNode = childNodes.item(i)
            if (childNode.nodeType != Node.ELEMENT_NODE
                || !childNode.hasChildNodes() && !childNode.hasAttributes()
            ) {
                continue  // The node contains no information, just ignore it.
            }
            val childElement = childNode as Element
            val tagName = childElement.tagName
            when (tagName) {
                SVG_PATH, SVG_RECT, SVG_CIRCLE, SVG_ELLIPSE, SVG_POLYGON, SVG_POLYLINE, SVG_LINE -> {
                    val child = SvgLeafNode(svgTree, childElement, tagName + i)
                    processIdName(svgTree, child)
                    currentGroup.addChild(child)
                    extractAllItemsAs(svgTree, child, childElement, currentGroup)
                    svgTree.hasLeafNode = true
                }

                SVG_GROUP -> {
                    val childGroup = SvgGroupNode(svgTree, childElement, "child$i")
                    currentGroup.addChild(childGroup)
                    processIdName(svgTree, childGroup)
                    extractGroupNode(svgTree, childGroup, currentGroup)
                    traverseSvgAndExtract(svgTree, childGroup, childElement)
                }

                SVG_USE -> {
                    val childGroup = SvgGroupNode(svgTree, childElement, "child$i")
                    processIdName(svgTree, childGroup)
                    currentGroup.addChild(childGroup)
                    svgTree.addToPendingUseSet(childGroup)
                }

                SVG_DEFS -> {
                    val childGroup = SvgGroupNode(svgTree, childElement, "child$i")
                    traverseSvgAndExtract(svgTree, childGroup, childElement)
                }

                SVG_CLIP_PATH_ELEMENT, SVG_MASK -> {
                    val clipPath = SvgClipPathNode(svgTree, childElement, tagName + i)
                    processIdName(svgTree, clipPath)
                    traverseSvgAndExtract(svgTree, clipPath, childElement)
                }

                SVG_STYLE -> extractStyleNode(svgTree, childElement)
                "linearGradient" -> {
                    val gradientNode = SvgGradientNode(svgTree, childElement, tagName + i)
                    processIdName(svgTree, gradientNode)
                    extractGradientNode(svgTree, gradientNode)
                    gradientNode.fillPresentationAttributes("gradientType", "linear")
                    svgTree.hasGradient = true
                }

                "radialGradient" -> {
                    val gradientNode = SvgGradientNode(svgTree, childElement, tagName + i)
                    processIdName(svgTree, gradientNode)
                    extractGradientNode(svgTree, gradientNode)
                    gradientNode.fillPresentationAttributes("gradientType", "radial")
                    svgTree.hasGradient = true
                }

                else -> {
                    val id = childElement.getAttribute("id")
                    if (!id.isEmpty()) {
                        svgTree.addIgnoredId(id)
                    }
                    // For other fancy tags, like <switch>, they can contain children too.
                    // Report the unsupported nodes.
                    if (unsupportedSvgNodes.contains(tagName)) {
                        svgTree.logError("<$tagName> is not supported", childElement)
                    }
                    // This is a workaround for the cases using defs to define a full icon size clip
                    // path, which is redundant information anyway.
                    traverseSvgAndExtract(svgTree, currentGroup, childElement)
                }
            }
        }
    }

    /**
     * Reads content from a gradient element's documentNode and fills in attributes for the given
     * SVG gradient node.
     */
    private fun extractGradientNode(
        svg: SvgTree, gradientNode: SvgGradientNode
    ) {
        val element = gradientNode.documentElement
        val attrs = element.attributes
        if (attrs.getNamedItem(SVG_HREF) != null || attrs.getNamedItem(SVG_XLINK_HREF) != null) {
            svg.addToPendingGradientRefSet(gradientNode)
        }
        val len = attrs.length
        for (j in 0 until len) {
            val n = attrs.item(j)
            val name = n.nodeName
            val value = n.nodeValue
            if (gradientMap.containsKey(name)) {
                gradientNode.fillPresentationAttributes(name, value)
            }
        }
        val gradientChildren = element.childNodes

        // Default SVG gradient offset is the previous largest offset.
        var greatestOffset = 0.0
        for (i in 0 until gradientChildren.length) {
            val node = gradientChildren.item(i)
            val nodeName = node.nodeName
            if (nodeName == "stop") {
                val stopAttr = node.attributes
                // Default SVG gradient stop color is black.
                var color: String? = "rgb(0,0,0)"
                // Default SVG gradient stop opacity is 1.
                var opacity: String? = "1"
                for (k in 0 until stopAttr.length) {
                    val stopItem = stopAttr.item(k)
                    val name = stopItem.nodeName
                    val value = stopItem.nodeValue
                    try {
                        when (name) {
                            "offset" ->                                 // If a gradient's value is not greater than all previous offset
                                // values, then the offset value is adjusted to be equal to
                                // the largest of all previous offset values.
                                greatestOffset = extractOffset(value, greatestOffset)

                            "stop-color" -> color = value
                            "stop-opacity" -> opacity = value
                            "style" -> {
                                val parts = value.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                                for (attr in parts) {
                                    val splitAttribute =
                                        attr.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                                    if (splitAttribute.size == 2) {
                                        if (attr.startsWith("stop-color")) {
                                            color = splitAttribute[1]
                                        } else if (attr.startsWith("stop-opacity")) {
                                            opacity = splitAttribute[1]
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: IllegalArgumentException) {
                        val msg = String.format("Invalid attribute value: %s=\"%s\"", name, value)
                        svg.logError(msg, node)
                    }
                }
                val offset = svg.formatCoordinate(greatestOffset)
                val vdColor = gradientNode.colorSvg2Vd(color!!, "#000000")
                if (vdColor != null) {
                    color = vdColor
                }
                gradientNode.addGradientStop(color, offset, opacity!!)
            }
        }
    }

    /**
     * Finds the gradient offset value given a String containing the value and greatest previous
     * offset value.
     *
     * @param offset an absolute floating point value or a percentage
     * @param greatestOffset is the greatest offset value seen in the gradient so far
     * @return the new greatest offset value
     */
    private fun extractOffset(offset: String, greatestOffset: Double): Double {
        var x: Double
        x = if (offset.endsWith("%")) {
            offset.substring(0, offset.length - 1).toDouble() / 100
        } else {
            offset.toDouble()
        }
        // Gradient offset values must be between 0 and 1 or 0% and 100%.
        x = Math.min(1.0, Math.max(x, 0.0))
        return Math.max(x, greatestOffset)
    }

    /**
     * Checks to see if the childGroup references any clipPath or style elements. Saves the
     * reference in the svgTree to add the information to an SvgNode later.
     */
    private fun extractGroupNode(
        svgTree: SvgTree,
        childGroup: SvgGroupNode,
        currentGroup: SvgGroupNode
    ) {
        val a = childGroup.documentElement.attributes
        val len = a.length
        for (j in 0 until len) {
            val n = a.item(j)
            val name = n.nodeName
            val value = n.nodeValue
            if (name == SVG_CLIP_PATH || name == SVG_MASK) {
                if (!value.isEmpty()) {
                    svgTree.addClipPathAffectedNode(childGroup, currentGroup, value)
                }
            } else if (name == "class") {
                if (!value.isEmpty()) {
                    svgTree.addAffectedNodeToStyleClass(".$value", childGroup)
                }
            }
        }
    }

    /**
     * Extracts the attribute information from a style element and adds to the
     * styleClassAttributeMap of the SvgTree. SvgNodes reference style elements using a 'class'
     * attribute. The style attribute will be filled into the tree after the svgTree calls
     * traverseSVGAndExtract().
     */
    private fun extractStyleNode(svgTree: SvgTree, currentNode: Node) {
        val a = currentNode.childNodes
        val len = a.length
        var styleData = ""
        for (j in 0 until len) {
            val n = a.item(j)
            if (n.nodeType == Node.CDATA_SECTION_NODE || len == 1) {
                styleData = n.nodeValue
            }
        }
        if (!styleData.isEmpty()) {
            // Separate each of the classes.
            val classData = styleData.split("}".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (aClassData in classData) {
                // Separate the class name from the attribute values.
                val splitClassData = aClassData.split("\\{".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (splitClassData.size < 2) {
                    // When the class info is empty, then skip.
                    continue
                }
                var className = splitClassData[0].trim { it <= ' ' }
                val styleAttr = splitClassData[1].trim { it <= ' ' }
                // Separate multiple classes if necessary.
                val splitClassNames = className.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (splitClassName in splitClassNames) {
                    var styleAttrTemp = styleAttr
                    className = splitClassName.trim { it <= ' ' }
                    // Concatenate the attributes to existing attributes.
                    val existing = svgTree.getStyleClassAttr(className)
                    if (existing != null) {
                        styleAttrTemp += ";$existing"
                    }
                    svgTree.addStyleClassToTree(className, styleAttrTemp)
                }
            }
        }
    }

    /**
     * Checks if the id of a node exists and adds the id and SvgNode to the svgTree's idMap if it
     * exists.
     */
    private fun processIdName(svgTree: SvgTree, node: SvgNode) {
        val id = node.getAttributeValue("id")
        if (!id.isEmpty()) {
            svgTree.addIdToMap(id, node)
        }
    }

    /**
     * Replaces an SvgNode in the SvgTree that references a clipPath element with the
     * SvgClipPathNode that corresponds to the referenced clip-path id. Adds the SvgNode as an
     * affected node of the SvgClipPathNode.
     */
    private fun handleClipPath(
        svg: SvgTree,
        child: SvgNode,
        currentGroup: SvgGroupNode?,
        value: String?
    ) {
        if (currentGroup == null || value == null) {
            return
        }
        val clipName = getClipPathName(value) ?: return
        val clipNode = svg.getSvgNodeFromId(clipName) ?: return
        val clipCopy = (clipNode as SvgClipPathNode).deepCopy()
        currentGroup.replaceChild(child, clipCopy)
        clipCopy.addAffectedNode(child)
        clipCopy.setClipPathNodeAttributes()
    }

    /**
     * Normally, clip path is referred as "url(#clip-path)", this function can help to extract the
     * name, which is "clip-path" here.
     *
     * @return the name of the clip path or null if the given string does not contain a proper clip
     * path name.
     */
    private fun getClipPathName(s: String?): String? {
        if (s == null) {
            return null
        }
        val startPos = s.indexOf('#')
        var endPos = s.indexOf(')', startPos + 1)
        if (endPos < 0) {
            endPos = s.length
        }
        return s.substring(startPos + 1, endPos).trim { it <= ' ' }
    }

    /** Reads the content from currentItem and fills into the SvgLeafNode "child".  */
    private fun extractAllItemsAs(
        svg: SvgTree,
        child: SvgLeafNode,
        currentItem: Node,
        currentGroup: SvgGroupNode
    ) {
        var parentNode = currentItem.parentNode
        var hasNodeAttr = false
        var styleContent = ""
        val styleContentBuilder = StringBuilder()
        var nothingToDisplay = false
        while (parentNode != null && parentNode.nodeName == "g") {
            // Parse the group's attributes.
            logger.log(Level.FINE, "Printing current parent")
            printlnCommon(parentNode)
            val attr = parentNode.attributes
            val nodeAttr = attr.getNamedItem(SVG_STYLE)
            // Search for the "display:none", if existed, then skip this item.
            if (nodeAttr != null) {
                styleContentBuilder.append(nodeAttr.textContent)
                styleContentBuilder.append(';')
                styleContent = styleContentBuilder.toString()
                logger.log(Level.FINE, "styleContent is :" + styleContent + "at number group ")
                if (styleContent.contains("display:none")) {
                    logger.log(Level.FINE, "Found none style, skip the whole group")
                    nothingToDisplay = true
                    break
                } else {
                    hasNodeAttr = true
                }
            }
            val displayAttr = attr.getNamedItem(SVG_DISPLAY)
            if (displayAttr != null && "none" == displayAttr.nodeValue) {
                logger.log(Level.FINE, "Found display:none style, skip the whole group")
                nothingToDisplay = true
                break
            }
            parentNode = parentNode.parentNode
        }
        if (nothingToDisplay) {
            // Skip this current whole item.
            return
        }
        logger.log(Level.FINE, "Print current item")
        printlnCommon(currentItem)
        if (hasNodeAttr && !styleContent.isEmpty()) {
            addStyleToPath(child, styleContent)
        }
        if (SVG_PATH == currentItem.nodeName) {
            extractPathItem(svg, child, currentItem, currentGroup)
        }
        if (SVG_RECT == currentItem.nodeName) {
            extractRectItem(svg, child, currentItem, currentGroup)
        }
        if (SVG_CIRCLE == currentItem.nodeName) {
            extractCircleItem(svg, child, currentItem, currentGroup)
        }
        if (SVG_POLYGON == currentItem.nodeName || SVG_POLYLINE == currentItem.nodeName) {
            extractPolyItem(svg, child, currentItem, currentGroup)
        }
        if (SVG_LINE == currentItem.nodeName) {
            extractLineItem(svg, child, currentItem, currentGroup)
        }
        if (SVG_ELLIPSE == currentItem.nodeName) {
            extractEllipseItem(svg, child, currentItem, currentGroup)
        }

        // Add the type of node as a style class name for child.
        svg.addAffectedNodeToStyleClass(currentItem.nodeName, child)
    }

    private fun printlnCommon(n: Node) {
        logger.log(Level.FINE, " nodeName=\"" + n.nodeName + "\"")
        var `val` = n.namespaceURI
        if (`val` != null) {
            logger.log(Level.FINE, " uri=\"$`val`\"")
        }
        `val` = n.prefix
        if (`val` != null) {
            logger.log(Level.FINE, " pre=\"$`val`\"")
        }
        `val` = n.localName
        if (`val` != null) {
            logger.log(Level.FINE, " local=\"$`val`\"")
        }
        `val` = n.nodeValue
        if (`val` != null) {
            logger.log(Level.FINE, " nodeValue=")
            if (`val`.trim { it <= ' ' }.isEmpty()) {
                // Whitespace
                logger.log(Level.FINE, "[WS]")
            } else {
                logger.log(Level.FINE, "\"" + n.nodeValue + "\"")
            }
        }
    }

    /** Convert polygon element into a path.  */
    private fun extractPolyItem(
        svgTree: SvgTree,
        child: SvgLeafNode,
        currentGroupNode: Node,
        currentGroup: SvgGroupNode
    ) {
        logger.log(Level.FINE, "Polyline or Polygon found" + currentGroupNode.textContent)
        if (currentGroupNode.nodeType == Node.ELEMENT_NODE) {
            val attributes = currentGroupNode.attributes
            val len = attributes.length
            for (itemIndex in 0 until len) {
                val n = attributes.item(itemIndex)
                val name = n.nodeName
                val value = n.nodeValue
                try {
                    if (name == SVG_STYLE) {
                        addStyleToPath(child, value)
                    } else if (presentationMap.containsKey(name)) {
                        child.fillPresentationAttributes(name, value)
                    } else if (name == SVG_CLIP_PATH || name == SVG_MASK) {
                        svgTree.addClipPathAffectedNode(child, currentGroup, value)
                    } else if (name == SVG_POINTS) {
                        val builder = PathBuilder()
                        val split = SPACE_OR_COMMA.split(value)
                        var baseX = split[0].toFloat()
                        var baseY = split[1].toFloat()
                        builder.absoluteMoveTo(baseX.toDouble(), baseY.toDouble())
                        var j = 2
                        while (j < split.size) {
                            val x = split[j].toFloat()
                            val y = split[j + 1].toFloat()
                            builder.relativeLineTo((x - baseX).toDouble(), (y - baseY).toDouble())
                            baseX = x
                            baseY = y
                            j += 2
                        }
                        if (SVG_POLYGON == currentGroupNode.nodeName) {
                            builder.relativeClose()
                        }
                        child.setPathData(builder.toString())
                    } else if (name == "class") {
                        svgTree.addAffectedNodeToStyleClass(".$value", child)
                        svgTree.addAffectedNodeToStyleClass(
                            currentGroupNode.nodeName + "." + value, child
                        )
                    }
                } catch (e: NumberFormatException) {
                    svgTree.logError("Invalid value of \"$name\" attribute", n)
                } catch (e: ArrayIndexOutOfBoundsException) {
                    svgTree.logError("Invalid value of \"$name\" attribute", n)
                }
            }
        }
    }

    /** Convert rectangle element into a path.  */
    private fun extractRectItem(
        svg: SvgTree,
        child: SvgLeafNode,
        currentGroupNode: Node,
        currentGroup: SvgGroupNode
    ) {
        logger.log(Level.FINE, "Rect found" + currentGroupNode.textContent)
        if (currentGroupNode.nodeType == Node.ELEMENT_NODE) {
            var x = 0.0
            var y = 0.0
            var width = Double.NaN
            var height = Double.NaN
            var rx = 0.0
            var ry = 0.0
            val a = currentGroupNode.attributes
            val len = a.length
            var pureTransparent = false
            for (j in 0 until len) {
                val n = a.item(j)
                val name = n.nodeName
                val value = n.nodeValue
                try {
                    if (name == SVG_STYLE) {
                        addStyleToPath(child, value)
                        if (value.contains("opacity:0;")) {
                            pureTransparent = true
                        }
                    } else if (presentationMap.containsKey(name)) {
                        child.fillPresentationAttributes(name, value)
                    } else if (name == SVG_CLIP_PATH || name == SVG_MASK) {
                        svg.addClipPathAffectedNode(child, currentGroup, value)
                    } else if (name == "x") {
                        x = svg.parseXValue(value)
                    } else if (name == "y") {
                        y = svg.parseYValue(value)
                    } else if (name == "rx") {
                        rx = svg.parseXValue(value)
                    } else if (name == "ry") {
                        ry = svg.parseYValue(value)
                    } else if (name == "width") {
                        width = svg.parseXValue(value)
                    } else if (name == "height") {
                        height = svg.parseYValue(value)
                    } else if (name == "class") {
                        svg.addAffectedNodeToStyleClass("rect.$value", child)
                        svg.addAffectedNodeToStyleClass(".$value", child)
                    }
                } catch (e: IllegalArgumentException) {
                    val msg = String.format("Invalid attribute value: %s=\"%s\"", name, value)
                    svg.logError(msg, currentGroupNode)
                }
            }
            if (!pureTransparent
                && !java.lang.Double.isNaN(x)
                && !java.lang.Double.isNaN(y)
                && !java.lang.Double.isNaN(width)
                && !java.lang.Double.isNaN(height)
            ) {
                val builder = PathBuilder()
                if (rx <= 0 && ry <= 0) {
                    // "M x, y h width v height h -width z"
                    builder.absoluteMoveTo(x, y)
                    builder.relativeHorizontalTo(width)
                    builder.relativeVerticalTo(height)
                    builder.relativeHorizontalTo(-width)
                } else {
                    // Refer to http://www.w3.org/TR/SVG/shapes.html#RectElement
                    assert(rx > 0 || ry > 0)
                    if (ry == 0.0) {
                        ry = rx
                    } else if (rx == 0.0) {
                        rx = ry
                    }
                    if (rx > width / 2) rx = width / 2
                    if (ry > height / 2) ry = height / 2
                    builder.absoluteMoveTo(x + rx, y)
                    builder.absoluteLineTo(x + width - rx, y)
                    builder.absoluteArcTo(rx, ry, false, false, true, x + width, y + ry)
                    builder.absoluteLineTo(x + width, y + height - ry)
                    builder.absoluteArcTo(rx, ry, false, false, true, x + width - rx, y + height)
                    builder.absoluteLineTo(x + rx, y + height)
                    builder.absoluteArcTo(rx, ry, false, false, true, x, y + height - ry)
                    builder.absoluteLineTo(x, y + ry)
                    builder.absoluteArcTo(rx, ry, false, false, true, x + rx, y)
                }
                builder.relativeClose()
                child.setPathData(builder.toString())
            }
        }
    }

    /** Converts circle element into a path.  */
    private fun extractCircleItem(
        svg: SvgTree,
        child: SvgLeafNode,
        currentGroupNode: Node,
        currentGroup: SvgGroupNode
    ) {
        logger.log(Level.FINE, "circle found" + currentGroupNode.textContent)
        if (currentGroupNode.nodeType == Node.ELEMENT_NODE) {
            var cx = 0f
            var cy = 0f
            var radius = 0f
            val a = currentGroupNode.attributes
            val len = a.length
            var pureTransparent = false
            for (j in 0 until len) {
                val n = a.item(j)
                val name = n.nodeName
                val value = n.nodeValue
                if (name == SVG_STYLE) {
                    addStyleToPath(child, value)
                    if (value.contains("opacity:0;")) {
                        pureTransparent = true
                    }
                } else if (presentationMap.containsKey(name)) {
                    child.fillPresentationAttributes(name, value)
                } else if (name == SVG_CLIP_PATH || name == SVG_MASK) {
                    svg.addClipPathAffectedNode(child, currentGroup, value)
                } else if (name == "cx") {
                    cx = value.toFloat()
                } else if (name == "cy") {
                    cy = value.toFloat()
                } else if (name == "r") {
                    radius = value.toFloat()
                } else if (name == "class") {
                    svg.addAffectedNodeToStyleClass("circle.$value", child)
                    svg.addAffectedNodeToStyleClass(".$value", child)
                }
            }
            if (!pureTransparent && !java.lang.Float.isNaN(cx) && !java.lang.Float.isNaN(cy)) {
                // "M cx cy m -r, 0 a r,r 0 1,1 (r * 2),0 a r,r 0 1,1 -(r * 2),0"
                val builder = PathBuilder()
                builder.absoluteMoveTo(cx.toDouble(), cy.toDouble())
                builder.relativeMoveTo(-radius.toDouble(), 0.0)
                builder.relativeArcTo(
                    radius.toDouble(),
                    radius.toDouble(),
                    false,
                    true,
                    true,
                    (2 * radius).toDouble(),
                    0.0
                )
                builder.relativeArcTo(
                    radius.toDouble(),
                    radius.toDouble(),
                    false,
                    true,
                    true,
                    (-2 * radius).toDouble(),
                    0.0
                )
                child.setPathData(builder.toString())
            }
        }
    }

    /** Convert ellipse element into a path.  */
    private fun extractEllipseItem(
        svg: SvgTree,
        child: SvgLeafNode,
        currentGroupNode: Node,
        currentGroup: SvgGroupNode
    ) {
        logger.log(Level.FINE, "ellipse found" + currentGroupNode.textContent)
        if (currentGroupNode.nodeType == Node.ELEMENT_NODE) {
            var cx = 0f
            var cy = 0f
            var rx = 0f
            var ry = 0f
            val a = currentGroupNode.attributes
            val len = a.length
            var pureTransparent = false
            for (j in 0 until len) {
                val n = a.item(j)
                val name = n.nodeName
                val value = n.nodeValue
                if (name == SVG_STYLE) {
                    addStyleToPath(child, value)
                    if (value.contains("opacity:0;")) {
                        pureTransparent = true
                    }
                } else if (presentationMap.containsKey(name)) {
                    child.fillPresentationAttributes(name, value)
                } else if (name == SVG_CLIP_PATH || name == SVG_MASK) {
                    svg.addClipPathAffectedNode(child, currentGroup, value)
                } else if (name == "cx") {
                    cx = value.toFloat()
                } else if (name == "cy") {
                    cy = value.toFloat()
                } else if (name == "rx") {
                    rx = value.toFloat()
                } else if (name == "ry") {
                    ry = value.toFloat()
                } else if (name == "class") {
                    svg.addAffectedNodeToStyleClass("ellipse.$value", child)
                    svg.addAffectedNodeToStyleClass(".$value", child)
                }
            }
            if (!pureTransparent && !java.lang.Float.isNaN(cx) && !java.lang.Float.isNaN(cy) && rx > 0 && ry > 0) {
                // "M cx -rx, cy a rx,ry 0 1,0 (rx * 2),0 a rx,ry 0 1,0 -(rx * 2),0"
                val builder = PathBuilder()
                builder.absoluteMoveTo((cx - rx).toDouble(), cy.toDouble())
                builder.relativeArcTo(rx.toDouble(), ry.toDouble(), false, true, false, (2 * rx).toDouble(), 0.0)
                builder.relativeArcTo(rx.toDouble(), ry.toDouble(), false, true, false, (-2 * rx).toDouble(), 0.0)
                builder.relativeClose()
                child.setPathData(builder.toString())
            }
        }
    }

    /** Convert line element into a path.  */
    private fun extractLineItem(
        svg: SvgTree,
        child: SvgLeafNode,
        currentGroupNode: Node,
        currentGroup: SvgGroupNode
    ) {
        logger.log(Level.FINE, "line found" + currentGroupNode.textContent)
        if (currentGroupNode.nodeType == Node.ELEMENT_NODE) {
            var x1 = 0f
            var y1 = 0f
            var x2 = 0f
            var y2 = 0f
            val a = currentGroupNode.attributes
            val len = a.length
            var pureTransparent = false
            for (j in 0 until len) {
                val n = a.item(j)
                val name = n.nodeName
                val value = n.nodeValue
                if (name == SVG_STYLE) {
                    addStyleToPath(child, value)
                    if (value.contains("opacity:0;")) {
                        pureTransparent = true
                    }
                } else if (presentationMap.containsKey(name)) {
                    child.fillPresentationAttributes(name, value)
                } else if (name == SVG_CLIP_PATH || name == SVG_MASK) {
                    svg.addClipPathAffectedNode(child, currentGroup, value)
                } else if (name == "x1") {
                    x1 = value.toFloat()
                } else if (name == "y1") {
                    y1 = value.toFloat()
                } else if (name == "x2") {
                    x2 = value.toFloat()
                } else if (name == "y2") {
                    y2 = value.toFloat()
                } else if (name == "class") {
                    svg.addAffectedNodeToStyleClass("line.$value", child)
                    svg.addAffectedNodeToStyleClass(".$value", child)
                }
            }
            if (!pureTransparent
                && !java.lang.Float.isNaN(x1)
                && !java.lang.Float.isNaN(y1)
                && !java.lang.Float.isNaN(x2)
                && !java.lang.Float.isNaN(y2)
            ) {
                // "M x1, y1 L x2, y2"
                val builder = PathBuilder()
                builder.absoluteMoveTo(x1.toDouble(), y1.toDouble())
                builder.absoluteLineTo(x2.toDouble(), y2.toDouble())
                child.setPathData(builder.toString())
            }
        }
    }

    private fun extractPathItem(
        svg: SvgTree,
        child: SvgLeafNode,
        currentGroupNode: Node,
        currentGroup: SvgGroupNode
    ) {
        logger.log(Level.FINE, "Path found " + currentGroupNode.textContent)
        if (currentGroupNode.nodeType == Node.ELEMENT_NODE) {
            val a = currentGroupNode.attributes
            val len = a.length
            for (j in 0 until len) {
                val n = a.item(j)
                val name = n.nodeName
                val value = n.nodeValue
                if (name == SVG_STYLE) {
                    addStyleToPath(child, value)
                } else if (presentationMap.containsKey(name)) {
                    child.fillPresentationAttributes(name, value)
                } else if (name == SVG_CLIP_PATH || name == SVG_MASK) {
                    svg.addClipPathAffectedNode(child, currentGroup, value)
                } else if (name == SVG_D) {
                    val pathData = Pattern.compile("(\\d)-").matcher(value).replaceAll("$1,-")
                    child.setPathData(pathData)
                } else if (name == "class") {
                    svg.addAffectedNodeToStyleClass("path.$value", child)
                    svg.addAffectedNodeToStyleClass(".$value", child)
                }
            }
        }
    }

    private fun addStyleToPath(path: SvgNode, value: String?) {
        logger.log(Level.FINE, "Style found is $value")
        if (value != null) {
            val parts = value.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            var k = parts.size
            while (--k >= 0) {
                val subStyle = parts[k]
                val nameValue: Array<String?> = subStyle.split(":".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                if (nameValue.size == 2 && nameValue[0] != null && nameValue[1] != null) {
                    val attr = nameValue[0]!!.trim { it <= ' ' }
                    val `val` = nameValue[1]!!.trim { it <= ' ' }
                    if (presentationMap.containsKey(attr)) {
                        path.fillPresentationAttributes(attr, `val`)
                    } else if (attr == SVG_OPACITY) {
                        // TODO: This is hacky, since we don't have a group level android:opacity.
                        //       This only works when the paths don't overlap.
                        path.fillPresentationAttributes(SVG_FILL_OPACITY, nameValue[1]!!)
                    }

                    // We need to handle a clip-path or a mask within the style in a different way
                    // than other styles. We treat it as an attribute clip-path = "#url(name)".
                    if (attr == SVG_CLIP_PATH || attr == SVG_MASK) {
                        val parentNode = path.tree.findParent(path)
                        if (parentNode != null) {
                            path.tree.addClipPathAffectedNode(path, parentNode, `val`)
                        }
                    }
                }
            }
        }
    }

    @JvmStatic
    fun parseFloatOrDefault(value: String, defaultValue: Float): Float {
        if (!value.isEmpty()) {
            try {
                return value.toFloat()
            } catch (ignore: NumberFormatException) {
            }
        }
        return defaultValue
    }

    @Throws(IOException::class)
    private fun writeFile(outStream: OutputStream, svgTree: SvgTree) {
        svgTree.writeXml(outStream)
    }

    /**
     * Converts an SVG file into VectorDrawable's XML content, if no error is found.
     *
     * @param inputSvg the input SVG file
     * @param outStream the converted VectorDrawable's content. This can be empty if there is any
     * error found during parsing
     * @return the error message that combines all logged errors and warnings, or an empty string if
     * there were no errors
     */
    @Throws(IOException::class)
    fun parseSvgToXml(inputSvg: Path, outStream: OutputStream): String {
        val inputStream = BufferedInputStream(Files.newInputStream(inputSvg))
        val svgTree = parse(inputSvg.fileName.toString(), inputStream)
        if (svgTree.hasLeafNode) {
            writeFile(outStream, svgTree)
        }
        return svgTree.errorMessage
    }

    private fun toVector(svgTree: SvgTree): ImageVector {
        val builder = ImageVector.Builder(
            defaultWidth = (svgTree.width * svgTree.scaleFactor).toInt().dp,
            defaultHeight = (svgTree.height * svgTree.scaleFactor).toInt().dp,
            viewportWidth = svgTree.viewportWidth,
            viewportHeight = svgTree.viewportHeight
        )
        svgTree.normalize()
        // TODO: this has to happen in the tree mode!!!
        svgTree.root!!.buildVector(ImageVectorBuilder(builder))
        return builder.build()
    }

    fun parseSvgToVector(name: String, inputStream: InputStream): ImageVector {
        val svgTree = parse(name, inputStream)
        if (svgTree.hasLeafNode) {
            return toVector(svgTree)
        }
        return ImageVector.Builder(defaultWidth = 1.dp, defaultHeight = 1.dp, viewportWidth = 1f, viewportHeight = 1f).build()
    }

    fun sampleVector(inputStream: InputStream): ImageVector {
        inputStream.use { inputStream ->
            return parseSvgToVector("test.svg", inputStream)
        }
    }
}
