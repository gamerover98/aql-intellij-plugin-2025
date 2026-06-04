// ArangoDB initialization script for local development / plugin testing.
// Executed by arangosh against the _system database on first container start.
// To re-run: docker compose down -v && docker compose up -d

'use strict';

const graph_module = require('@arangodb/general-graph');

// ─── Create test database ────────────────────────────────────────────────────

if (!db._databases().includes('testdb')) {
    db._createDatabase('testdb');
}
db._useDatabase('testdb');

// ─── Document collections ────────────────────────────────────────────────────

const users     = db._collection('users')     || db._create('users');
const products  = db._collection('products')  || db._create('products');
const orders    = db._collection('orders')    || db._create('orders');
const reviews   = db._collection('reviews')   || db._create('reviews');
const tags      = db._collection('tags')      || db._create('tags');

// ─── Edge collections ────────────────────────────────────────────────────────

const order_items  = db._collection('order_items')  || db._createEdgeCollection('order_items');
const user_orders  = db._collection('user_orders')  || db._createEdgeCollection('user_orders');
const product_tags = db._collection('product_tags') || db._createEdgeCollection('product_tags');

// ─── Indexes ─────────────────────────────────────────────────────────────────

users.ensureIndex({ type: 'persistent', fields: ['email'], unique: true });
users.ensureIndex({ type: 'persistent', fields: ['city'] });
products.ensureIndex({ type: 'persistent', fields: ['category'] });
products.ensureIndex({ type: 'persistent', fields: ['price'] });
orders.ensureIndex({ type: 'persistent', fields: ['status'] });
orders.ensureIndex({ type: 'persistent', fields: ['createdAt'] });

// ─── Seed: users ─────────────────────────────────────────────────────────────

const userData = [
    { _key: 'u1', name: 'Alice Romano',   email: 'alice@example.com',  age: 28, city: 'Rome',     active: true,  role: 'admin'    },
    { _key: 'u2', name: 'Bob Ferreri',    email: 'bob@example.com',    age: 35, city: 'Milan',    active: true,  role: 'customer' },
    { _key: 'u3', name: 'Carol Esposito', email: 'carol@example.com',  age: 22, city: 'Naples',   active: false, role: 'customer' },
    { _key: 'u4', name: 'David Ricci',    email: 'david@example.com',  age: 41, city: 'Turin',    active: true,  role: 'customer' },
    { _key: 'u5', name: 'Emma Conti',     email: 'emma@example.com',   age: 31, city: 'Florence', active: true,  role: 'customer' },
    { _key: 'u6', name: 'Luca Barbieri',  email: 'luca@example.com',   age: 19, city: 'Venice',   active: true,  role: 'customer' },
    { _key: 'u7', name: 'Sofia Gallo',    email: 'sofia@example.com',  age: 44, city: 'Bologna',  active: true,  role: 'customer' },
    { _key: 'u8', name: 'Marco Bruno',    email: 'marco@example.com',  age: 27, city: 'Rome',     active: false, role: 'customer' },
];
for (const u of userData) {
    if (!users.exists(u._key)) { users.save(u); }
}

// ─── Seed: tags ──────────────────────────────────────────────────────────────

const tagData = [
    { _key: 't1', label: 'electronics' },
    { _key: 't2', label: 'bestseller'  },
    { _key: 't3', label: 'books'       },
    { _key: 't4', label: 'office'      },
    { _key: 't5', label: 'sale'        },
    { _key: 't6', label: 'furniture'   },
    { _key: 't7', label: 'accessories' },
];
for (const t of tagData) {
    if (!tags.exists(t._key)) { tags.save(t); }
}

// ─── Seed: products ──────────────────────────────────────────────────────────

const productData = [
    { _key: 'p1', name: 'Laptop Pro 15',       category: 'electronics', price: 1299.99, stock: 50,  rating: 4.7 },
    { _key: 'p2', name: 'Wireless Mouse X200', category: 'electronics', price:   29.99, stock: 200, rating: 4.2 },
    { _key: 'p3', name: 'AQL in Practice',     category: 'books',       price:   49.99, stock:  30, rating: 4.9 },
    { _key: 'p4', name: 'Ceramic Coffee Mug',  category: 'office',      price:   12.99, stock: 100, rating: 4.0 },
    { _key: 'p5', name: 'MechKey TKL',         category: 'electronics', price:   89.99, stock:  75, rating: 4.5 },
    { _key: 'p6', name: 'Bamboo Standing Desk',category: 'furniture',   price:  499.99, stock:  15, rating: 4.6 },
    { _key: 'p7', name: 'USB-C Hub 7-in-1',    category: 'electronics', price:   39.99, stock: 120, rating: 4.3 },
    { _key: 'p8', name: 'Notebook A5 Pack',    category: 'office',      price:    9.99, stock: 300, rating: 3.8 },
];
for (const p of productData) {
    if (!products.exists(p._key)) { products.save(p); }
}

// ─── Seed: product_tags edges (product → tag) ────────────────────────────────

const productTagData = [
    { _from: 'products/p1', _to: 'tags/t1' },
    { _from: 'products/p1', _to: 'tags/t2' },
    { _from: 'products/p2', _to: 'tags/t1' },
    { _from: 'products/p2', _to: 'tags/t7' },
    { _from: 'products/p3', _to: 'tags/t3' },
    { _from: 'products/p3', _to: 'tags/t2' },
    { _from: 'products/p4', _to: 'tags/t4' },
    { _from: 'products/p5', _to: 'tags/t1' },
    { _from: 'products/p5', _to: 'tags/t5' },
    { _from: 'products/p6', _to: 'tags/t6' },
    { _from: 'products/p7', _to: 'tags/t1' },
    { _from: 'products/p7', _to: 'tags/t7' },
    { _from: 'products/p8', _to: 'tags/t4' },
];
for (const e of productTagData) {
    product_tags.save(e);
}

// ─── Seed: orders ────────────────────────────────────────────────────────────

const orderData = [
    { _key: 'o1', status: 'completed',  total: 1329.98, createdAt: '2024-01-15', currency: 'EUR' },
    { _key: 'o2', status: 'processing', total:   49.99, createdAt: '2024-02-20', currency: 'EUR' },
    { _key: 'o3', status: 'completed',  total:  539.98, createdAt: '2024-03-05', currency: 'EUR' },
    { _key: 'o4', status: 'cancelled',  total:   29.99, createdAt: '2024-03-10', currency: 'EUR' },
    { _key: 'o5', status: 'completed',  total:   89.99, createdAt: '2024-04-01', currency: 'EUR' },
    { _key: 'o6', status: 'completed',  total:  129.97, createdAt: '2024-04-12', currency: 'EUR' },
    { _key: 'o7', status: 'processing', total:  499.99, createdAt: '2024-05-03', currency: 'EUR' },
];
for (const o of orderData) {
    if (!orders.exists(o._key)) { orders.save(o); }
}

// ─── Seed: user_orders edges (user → order) ──────────────────────────────────

const userOrderData = [
    { _from: 'users/u1', _to: 'orders/o1' },
    { _from: 'users/u2', _to: 'orders/o2' },
    { _from: 'users/u3', _to: 'orders/o3' },
    { _from: 'users/u4', _to: 'orders/o4' },
    { _from: 'users/u1', _to: 'orders/o5' },
    { _from: 'users/u5', _to: 'orders/o6' },
    { _from: 'users/u7', _to: 'orders/o7' },
];
for (const e of userOrderData) {
    user_orders.save(e);
}

// ─── Seed: order_items edges (order → product) ───────────────────────────────

const orderItemData = [
    { _from: 'orders/o1', _to: 'products/p1', quantity: 1, unitPrice: 1299.99 },
    { _from: 'orders/o1', _to: 'products/p2', quantity: 1, unitPrice:   29.99 },
    { _from: 'orders/o2', _to: 'products/p3', quantity: 1, unitPrice:   49.99 },
    { _from: 'orders/o3', _to: 'products/p6', quantity: 1, unitPrice:  499.99 },
    { _from: 'orders/o3', _to: 'products/p2', quantity: 2, unitPrice:   29.99 },
    { _from: 'orders/o4', _to: 'products/p2', quantity: 1, unitPrice:   29.99 },
    { _from: 'orders/o5', _to: 'products/p5', quantity: 1, unitPrice:   89.99 },
    { _from: 'orders/o6', _to: 'products/p4', quantity: 3, unitPrice:   12.99 },
    { _from: 'orders/o6', _to: 'products/p8', quantity: 9, unitPrice:    9.99 },
    { _from: 'orders/o7', _to: 'products/p6', quantity: 1, unitPrice:  499.99 },
];
for (const e of orderItemData) {
    order_items.save(e);
}

// ─── Seed: reviews ───────────────────────────────────────────────────────────

const reviewData = [
    { _key: 'r1', productKey: 'p1', userKey: 'u1', score: 5, body: 'Excellent build quality and performance.' },
    { _key: 'r2', productKey: 'p3', userKey: 'u2', score: 5, body: 'Best AQL resource I have found.'           },
    { _key: 'r3', productKey: 'p5', userKey: 'u1', score: 4, body: 'Great feel, solid actuation.'              },
    { _key: 'r4', productKey: 'p4', userKey: 'u5', score: 4, body: 'Nice mug, keeps coffee warm.'              },
    { _key: 'r5', productKey: 'p6', userKey: 'u7', score: 5, body: 'Worth every cent, delivery was fast.'      },
];
for (const r of reviewData) {
    if (!reviews.exists(r._key)) { reviews.save(r); }
}

// ─── Named graph ─────────────────────────────────────────────────────────────

const GRAPH_NAME = 'store_graph';
if (!graph_module._exists(GRAPH_NAME)) {
    graph_module._create(GRAPH_NAME, [
        graph_module._relation('user_orders',  ['users'],    ['orders']),
        graph_module._relation('order_items',  ['orders'],   ['products']),
        graph_module._relation('product_tags', ['products'], ['tags']),
    ]);
}

print('[init] testdb populated successfully.');
print('[init] Collections: users, products, orders, reviews, tags, order_items, user_orders, product_tags');
print('[init] Graph: store_graph');
