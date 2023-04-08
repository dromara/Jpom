/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
// base library
import {
	//   Base,
	//   Affix,
	//   Anchor,
	AutoComplete,
	Alert,
	Avatar,
	BackTop,
	Badge,
	//   Breadcrumb,
	Button,
	//   Calendar,
	Card,
	Collapse,
	//   Carousel,
	Cascader,
	Checkbox,
	Col,
	DatePicker,
	Divider,
	Dropdown,
	Form,
	FormModel,
	Icon,
	Input,
	InputNumber,
	Layout,
	List,
	LocaleProvider,
	Menu,
	//   Mentions,
	Modal,
	Pagination,
	Popconfirm,
	Popover,
	Progress,
	Radio,
	//   Rate,
	Row,
	Select,
	//   Slider,
	Spin,
	Statistic,
	Steps,
	Switch,
	Table,
	Transfer,
	Tree,
	TreeSelect,
	Tabs,
	Tag,
	TimePicker,
	Timeline,
	Tooltip,
	Upload,
	Drawer,
	//   Skeleton,
	//   Comment,
	// ColorPicker,
	ConfigProvider,
	Empty,
	Result,
	Descriptions,
	PageHeader,
	Space,
	message,
	notification,
} from "ant-design-vue";

const components = [
	//   Base,
	//   Affix,
	//   Anchor,
	AutoComplete,
	Alert,
	Avatar,
	BackTop,
	Badge,
	//   Breadcrumb,
	Button,
	//   Calendar,
	Card,
	Collapse,
	//   Carousel,
	Cascader,
	Checkbox,
	Col,
	DatePicker,
	Divider,
	Dropdown,
	Form,
	FormModel,
	Icon,
	Input,
	InputNumber,
	Layout,
	List,
	LocaleProvider,
	Menu,
	//   Mentions,
	Modal,
	Pagination,
	Popconfirm,
	Popover,
	Progress,
	Radio,
	//   Rate,
	Row,
	Select,
	//   Slider,
	Spin,
	Statistic,
	Steps,
	Switch,
	Table,
	Transfer,
	Tree,
	TreeSelect,
	Tabs,
	Tag,
	TimePicker,
	Timeline,
	Tooltip,
	Upload,
	Drawer,
	//   Skeleton,
	//   Comment,
	// ColorPicker,
	ConfigProvider,
	Empty,
	Result,
	Descriptions,
	PageHeader,
	Space,
];
components.map((component) => {
	Vue.use(component);
});

Vue.prototype.$message = message;
Vue.prototype.$notification = notification;
//
Vue.prototype.$confirm = Modal.confirm;
Vue.prototype.$info = Modal.info;
Vue.prototype.$error = Modal.error;
Vue.prototype.$warning = Modal.warning;
Vue.prototype.$success = Modal.success;
