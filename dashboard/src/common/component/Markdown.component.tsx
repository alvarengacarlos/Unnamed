import { Blockquote, Checkbox, Code, Divider, List, Stack, Table, Text, Title } from "@mantine/core"
import ReactMarkdown, { type Components } from "react-markdown"
import remarkGfm from "remark-gfm"

interface Props {
    children: string
}

/**
 * Markdown (GitHub Flavored) shown with the components of Mantine, such as the replies of the assistant. The text may
 * carry what someone typed into the data, so it is shown as text only: raw HTML is not rendered, and links and images
 * neither, so no URL of the text is opened or loaded (an image loads its URL by itself, which could send data out).
 * A link becomes its text; an image, nothing.
 */
export default function Markdown(props: Props) {
    return (
        <Stack gap="xs">
            <ReactMarkdown
                remarkPlugins={[remarkGfm]}
                components={components}
                skipHtml
                disallowedElements={["a", "img"]}
                unwrapDisallowed
            >
                {props.children}
            </ReactMarkdown>
        </Stack>
    )
}

// The node of each element is left out of the props: it is the syntax tree, not an attribute
const components: Components = {
    p: ({ children }) => <Text>{children}</Text>,
    h1: ({ children }) => <Title order={3}>{children}</Title>,
    h2: ({ children }) => <Title order={4}>{children}</Title>,
    h3: ({ children }) => <Title order={5}>{children}</Title>,
    h4: ({ children }) => <Title order={6}>{children}</Title>,
    h5: ({ children }) => <Title order={6}>{children}</Title>,
    h6: ({ children }) => <Title order={6}>{children}</Title>,
    strong: ({ children }) => <Text span fw={700}>{children}</Text>,
    em: ({ children }) => <Text span fs="italic">{children}</Text>,
    del: ({ children }) => <Text span td="line-through">{children}</Text>,
    ul: ({ children }) => <List>{children}</List>,
    ol: ({ children }) => <List type="ordered">{children}</List>,
    li: ({ children }) => <List.Item>{children}</List.Item>,
    // The checkboxes of the task lists (- [x] done), which only show the state
    input: ({ checked }) => <Checkbox checked={!!checked} readOnly size="xs" display="inline-block" mr="xs"/>,
    blockquote: ({ children }) => <Blockquote p="sm">{children}</Blockquote>,
    hr: () => <Divider/>,
    // A block of code is a pre with a code inside: the block is drawn here, so the code inside is plain text
    pre: ({ node }) => <Code block>{textOf(node)}</Code>,
    code: ({ children }) => <Code>{children}</Code>,
    table: ({ children }) => (
        <Table.ScrollContainer minWidth={400}>
            <Table withTableBorder withColumnBorders>{children}</Table>
        </Table.ScrollContainer>
    ),
    thead: ({ children }) => <Table.Thead>{children}</Table.Thead>,
    tbody: ({ children }) => <Table.Tbody>{children}</Table.Tbody>,
    tr: ({ children }) => <Table.Tr>{children}</Table.Tr>,
    th: ({ children }) => <Table.Th>{children}</Table.Th>,
    td: ({ children }) => <Table.Td>{children}</Table.Td>
}

interface TextNode {
    value?: string
    children?: Array<TextNode>
}

// The text of a node of the syntax tree and of the nodes inside it, without its markup
function textOf(node: TextNode | undefined): string {
    if (!node) {
        return ""
    }

    return node.value ?? (node.children ?? []).map(textOf).join("")
}
